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
              best-lag (apply max-key
                              #(autocorrelation centered %)
                              lags)]

          (/ (double sample-rate) best-lag))))))

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

            f0-values (f0-estimates samples sample-rate segment-size)]


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

        (println "Normalized amplitude:"
               (amplitude normalized-samples))

        (println "Normalized RMS:"
               (rms normalized-samples)))




      (println "File not found:" path))))