(ns neurovoice.core-test
  (:require [midje.sweet :refer :all]
            [neurovoice.core :refer :all]))


(fact "Prosecna vrednost signala"
      (average [2 4 6]) => 4)


(fact "Maksimalna amplituda"
      (amplitude [-2 1 3]) => 3)


(fact "RMS vrednost"
      (rms [3 3]) => 3.0)


(fact "Energija signala"
      (energy [1 2 3]) => 14)


(fact "Raspon signala"
      (peak-to-peak [-2 0 5]) => 7)


(fact "Analiza signala"
      (:energy (analyze-signal [1 2 3])) => 14)