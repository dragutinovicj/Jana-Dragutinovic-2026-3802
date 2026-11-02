(ns neurovoice.core
  (:gen-class))


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

  (let [signal [0.2 0.5 -0.3 0.7 -0.1]
        result (analyze-signal signal)]

    (println "NeuroVoice - Analiza audio signala")
    (println "Signal:" signal)

    (println "Prosecna vrednost:" (:average result))
    (println "Maksimalna amplituda:" (:amplitude result))
    (println "RMS:" (:rms result))
    (println "Energija:" (:energy result))
    (println "Peak-to-peak:" (:peak-to-peak result))))