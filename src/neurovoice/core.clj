(ns neurovoice.core
  (:require [clojure.java.io :as io])
  (:import [javax.sound.sampled AudioSystem
                                AudioFormat
                                AudioFormat$Encoding])
  (:gen-class))

(defn read-sample [data position]
  (let [low (bit-and 255 (aget data position))
        high (aget data (inc position))]
    (/ (+ low (bit-shift-left high 8))
       32768.0)))

(defn read-audio [file-path]
  (with-open [input (AudioSystem/getAudioInputStream
                      (io/file file-path))]

    (let [format (.getFormat input)
          channels (.getChannels format)
          sample-rate (.getSampleRate format)

          pcm-format (AudioFormat.
                       AudioFormat$Encoding/PCM_SIGNED
                       sample-rate
                       16
                       channels
                       (* 2 channels)
                       sample-rate
                       false)]

      (with-open [audio (AudioSystem/getAudioInputStream
                          pcm-format input)]

        (let [data (.readAllBytes audio)

              samples (mapv
                        (fn [i]
                          (/ (reduce +
                                     (map
                                       (fn [c]
                                         (read-sample
                                           data
                                           (+ i (* 2 c))))
                                       (range channels)))
                             channels))

                        (range 0
                               (alength data)
                               (* 2 channels)))]

          {:samples samples
           :sample-rate sample-rate})))))

;; Prosecna vrednost signala
(defn average [signal]
  (/ (reduce + signal) (count signal)))

(defn standard-deviation [values]
  (when (seq values)
    (let [mean-value (average values)
          squared-differences
          (map (fn [value]
                 (* (- value mean-value)
                    (- value mean-value)))
               values)]

      (Math/sqrt (average squared-differences)))))

;; Maksimalna amplituda
(defn amplitude [signal]
  (apply max (map abs signal)))


;; RMS vrednost signala
(defn rms [signal]
  (Math/sqrt
    (/ (reduce + (map #(* % %) signal))
       (count signal))))


;; Energija signala
(defn energy [signal]
  (reduce + (map #(* % %) signal)))


;; Raspon signala
(defn peak-to-peak [signal]
  (- (apply max signal)
     (apply min signal)))

(defn normalize-signal [signal]
  (let [peak (if (seq signal)
               (amplitude signal)
               0)]
    (if (zero? peak)
      (vec signal)
      (mapv #(/ % peak) signal))))

(defn autocorrelation [signal lag]
  (when (or (not (integer? lag)) (neg? lag))
    (throw (IllegalArgumentException.
             "Lag must be a non-negative integer")))

  (reduce + 0
          (map * signal (drop lag signal))))

(defn normalized-autocorrelation [signal lag]
  (when (or (not (integer? lag)) (neg? lag))
    (throw (IllegalArgumentException.
             "Lag must be a non-negative integer")))

  (let [first-part (take (- (count signal) lag) signal)
        second-part (drop lag signal)

        energy-first (energy first-part)
        energy-second (energy second-part)

        denominator (Math/sqrt (* energy-first energy-second))]

    (if (zero? denominator)
      0.0
      (/ (autocorrelation signal lag) denominator))))

(defn fundamental-frequency [signal sample-rate]
  (when (not (pos? sample-rate))
    (throw (IllegalArgumentException.
             "Sample rate must be positive")))

  (when (seq signal)
    (let [mean-value (average signal)
          centered (mapv #(- % mean-value) signal)

          min-lag (max 1 (int (Math/ceil (/ sample-rate 400.0))))
          max-lag (min (dec (count signal))
                       (int (Math/floor (/ sample-rate 80.0))))]

      (when (and (<= min-lag max-lag)
                 (> (rms centered) 0.01))

        (let [lags (range min-lag (inc max-lag))
              correlation (fn [lag]
                              (normalized-autocorrelation centered lag))

              peaks (filter
                      (fn [lag]
                              (and (> (correlation lag) 0.9)
                                   (> (correlation lag) (correlation (dec lag)))
                                   (>= (correlation lag) (correlation (inc lag)))))
                              lags)
              best-lag (first peaks)]

         (when best-lag
           (/ (double sample-rate) best-lag)))))))

(defn analyze-f0 [signal sample-rate]
  (let [frequency (fundamental-frequency signal sample-rate)]
    (when frequency
      (let [mean-value (average signal)
            centered (mapv #(- % mean-value) signal)
            lag (int (Math/round (/ sample-rate frequency)))
            correlation (normalized-autocorrelation centered lag)]
        {:frequency frequency
         :correlation correlation}))))

;; Analiza svih karakteristika
(defn analyze-signal [signal]
  {:average (average signal)
   :amplitude (amplitude signal)
   :rms (rms signal)
   :energy (energy signal)
   :peak-to-peak (peak-to-peak signal)})

(defn split-signal [signal segment-size]
  (when (<= segment-size 0)
    (throw (IllegalArgumentException.
             "Segment size must be positive")))
  (mapv vec (partition-all segment-size signal)))

(defn analyze-segments [signal segment-size]
  (mapv analyze-signal
        (split-signal signal segment-size)))

(defn f0-estimates [signal sample-rate segment-size]
  (->> (split-signal signal segment-size)
       (filter #(> (rms %) 0.01))
       (map #(fundamental-frequency % sample-rate))
       (remove nil?)
       vec))

(defn analyze-f0-segments [signal sample-rate segment-size]
  (->> (split-signal signal segment-size)
       (map-indexed
         (fn [index segment]
           (when-let [result (analyze-f0 segment sample-rate)]
             (assoc result
               :segment-index index
               :time-seconds (/ (* index segment-size)
                                (double sample-rate))))))
       (remove nil?)
       vec))

(defn f0-variability [f0-analysis]
  (let [frequencies (map :frequency f0-analysis)]
    (when (seq frequencies)
      {:mean-f0 (average frequencies)
       :std-f0 (standard-deviation frequencies)})))

(defn f0-changes [f0-analysis]
  (->> (partition 2 1 f0-analysis)
       (filter (fn [[first-segment second-segment]]
                 (= (:segment-index second-segment)
                    (inc (:segment-index first-segment)))))
       (map (fn [[first-segment second-segment]]
              (Math/abs
                 (double
                   (- (:frequency second-segment)
                      (:frequency first-segment))))))
       vec))

(defn f0-coverage [total-segments f0-analysis]
  (if (pos? total-segments)
    (* 100.0
       (/ (count f0-analysis) total-segments))
    0.0))

(defn cycle-peaks [signal sample-rate]
  (let [frequency (fundamental-frequency signal sample-rate)]
    (if frequency
      (let [expected-period (/ sample-rate frequency)
            min-distance (* 0.7 expected-period)
            threshold (* 0.2 (amplitude signal))

            peaks (->> (range 1 (dec (count signal)))
                       (filter
                         (fn [i]
                           (and (> (nth signal i) threshold)
                                (> (nth signal i)
                                   (nth signal (dec i)))
                                (>= (nth signal i)
                                    (nth signal (inc i)))))))]
        (reduce
          (fn [result peak]
            (if (or (empty? result)
                    (>= (- peak (last result)) min-distance))
              (conj result peak)
              result))
          []
          peaks))
      [])))

(defn cycle-periods [peaks sample-rate]
  (when-not (pos? sample-rate)
    (throw (IllegalArgumentException.
             "Sample rate must be positive")))
  (->> (partition 2 1 peaks)
       (map (fn [[first-peak second-peak]]
              (/ (- second-peak first-peak)
                 (double sample-rate))))
       vec))

(defn valid-cycle-periods [periods frequency]
  (if (and frequency (pos? frequency))
    (let [expected-period (/ 1.0 frequency)
          min-period (* 0.8 expected-period)
          max-period (* 1.2 expected-period)]

      (->> periods
           (filter #(<= min-period % max-period))
           vec))
    []))

;; Pokretanje programa
(defn -main [& args]

  (doseq [path ["resources/audio/healthy.wav"
                "resources/audio/parkinsons.wav"]]

    (if (.exists (io/file path))

      (let [{:keys [samples sample-rate]} (read-audio path)
            result (analyze-signal samples)

            segment-size (max 1 (int (* sample-rate 0.1)))
            segments (analyze-segments samples segment-size)
            normalized-samples (normalize-signal samples)

            f0-values (f0-estimates samples sample-rate segment-size)

            f0-analysis (analyze-f0-segments samples sample-rate segment-size)

            f0-variation (f0-variability f0-analysis)

            f0-differences (f0-changes f0-analysis)]




        (println "\nFile:" path)
        (println "Sample rate:" sample-rate "Hz")
        (println "Duration:" (/ (count samples) sample-rate) "seconds")
        (println "Average:" (:average result))
        (println "Amplitude:" (:amplitude result))
        (println "RMS:" (:rms result))
        (println "Energy:" (:energy result))
        (println "Peak-to-peak:" (:peak-to-peak result))

        (println "Number of segments:" (count segments))
        (println "First five RMS values:"
                 (mapv :rms (take 5 segments)))

        (println "Number of F0 estimates:" (count f0-values))

        (println "First ten F0 estimates (Hz):"
                 (vec (take 10 f0-values)))

        (when (seq f0-values)
          (println "Average F0 (Hz):" (average f0-values)))

        (when f0-variation
          (println "F0 standard deviation (Hz):"
                   (:std-f0 f0-variation)))

        (println "Number of F0 analyses:" (count f0-analysis))

        (println "F0 coverage (%):"
                 (f0-coverage (count segments) f0-analysis))

        (println "First five F0 analyses:"
                 (vec (take 5 f0-analysis)))

        (when (seq f0-analysis)
          (println "Average correlation:"
                   (average (map :correlation f0-analysis))))

        (when (seq f0-differences)
          (println "Mean absolute F0 chabge (Hz):"
                   (average f0-differences)))

        (println "Normalized amplitude:"
               (amplitude normalized-samples))

        (println "Normalized RMS:"
               (rms normalized-samples))

        (when (seq f0-analysis)
          (let [first-result (first f0-analysis)
                first-index (:segment-index first-result)

                segment (nth (split-signal samples segment-size)
                             first-index)

                frequency (:frequency first-result)

                peaks (cycle-peaks segment sample-rate)
                periods (cycle-periods peaks sample-rate)

                valid-periods (valid-cycle-periods periods frequency)]

            (println "Number of detected cycle peaks:"
                     (count peaks))

            (println "First five cycle peaks:"
                     (vec (take 5 peaks)))

            (println "First five cycle periods (seconds):"
                     (vec (take 5 periods)))

            (println "Expected cycle period (seconds):"
                     (/ 1.0 frequency))

            (println "Number of cycle intervals:"
                     (count periods))

            (println "Number of valid cycle intervals:"
                     (count valid-periods)))))


      (println "File not found:" path))))