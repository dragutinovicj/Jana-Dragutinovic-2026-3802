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


;; Analiza svih karakteristika
(defn analyze-signal [signal]
  {:average (average signal)
   :amplitude (amplitude signal)
   :rms (rms signal)
   :energy (energy signal)
   :peak-to-peak (peak-to-peak signal)})


;; Pokretanje programa
(defn -main [& args]

  (doseq [path ["resources/audio/healthy.wav"
                "resources/audio/parkinsons.wav"]]

    (if (.exists (io/file path))

      (let [{:keys [samples sample-rate]} (read-audio path)
            result (analyze-signal samples)]

        (println "\nFile:" path)
        (println "Sample rate:" sample-rate "Hz")
        (println "Duration:" (/ (count samples) sample-rate) "seconds")
        (println "Average:" (:average result))
        (println "Amplitude:" (:amplitude result))
        (println "RMS:" (:rms result))
        (println "Energy:" (:energy result))
        (println "Peak-to-peak:" (:peak-to-peak result)))

      (println "File not found:" path))))