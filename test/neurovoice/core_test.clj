(ns neurovoice.core-test
  (:require [midje.sweet :refer :all]
            [neurovoice.core :as nv])
  (:import [java.io ByteArrayInputStream]
           [javax.sound.sampled AudioFormat
                                AudioInputStream
                                AudioSystem
                                AudioFileFormat$Type]))


(fact "Average signal value"
      (nv/average [2 4 6]) => 4)


(fact "Maximum signal amplitude"
      (nv/amplitude [-2 1 3]) => 3)


(fact "RMS signal value"
      (nv/rms [3 3]) => (roughly 3.0 0.0001))


(fact "Signal energy"
      (nv/energy [1 2 3]) => 14)


(fact "Peak-to-peak amplitude"
      (nv/peak-to-peak [-2 0 5]) => 7)


(fact "Complete signal analysis"
      (:energy (nv/analyze-signal [1 2 3])) => 14)

(fact "Normalizing signal amplitude"
      (nv/normalize-signal [2.0 -4.0 1.0])
      => [0.5 -1.0 0.25])


(fact "Normalizing a silent signal"
      (nv/normalize-signal [0.0 0.0 0.0])
      => [0.0 0.0 0.0])


(fact "Normalizing an empty signal"
      (nv/normalize-signal [])
      => [])


(fact "Reading 16-bit audio samples"
      (nv/read-sample (byte-array [0 64]) 0) => 0.5
      (nv/read-sample (byte-array [0 -64]) 0) => -0.5)


(fact "Reading a WAV audio file"
      (let [data (byte-array [0 0 0 64 0 -64])
            format (AudioFormat. (float 8000) 16 1 true false)
            file (java.io.File/createTempFile
                   "neurovoice-test-" ".wav")]

        (try
          (with-open [input (AudioInputStream.
                              (ByteArrayInputStream. data)
                              format
                              3)]
            (AudioSystem/write
              input AudioFileFormat$Type/WAVE file))

          (let [audio (nv/read-audio (.getAbsolutePath file))]
            (:sample-rate audio) => (roughly 8000.0 0.01)
            (:samples audio) => [0.0 0.5 -0.5])

          (finally
            (.delete file)))))

(fact "Splitting signal into segments"
      (nv/split-signal [1 2 3 4 5] 2)
      => [[1 2] [3 4] [5]])

(fact "Analyzing signal segments"
      (mapv :average
            (nv/analyze-segments [2 4 6 8] 2))
      => [3 7])

(fact "Autocorrelation with zero lag"
      (nv/autocorrelation [1 2 3] 0) => 14)


(fact "Autocorrelation with one sample lag"
      (nv/autocorrelation [1 2 3] 1) => 8)


(fact "Autocorrelation with two samples lag"
      (nv/autocorrelation [1 2 3] 2) => 3)


(fact "Autocorrelation of a periodic signal"
      (nv/autocorrelation [1 0 -1 0 1 0 -1 0] 4) => 2)


(fact "Autocorrelation of an empty signal"
      (nv/autocorrelation [] 0) => 0)


(fact "Autocorrelation with invalid lag"
      (nv/autocorrelation [1 2 3] -1)
      => (throws IllegalArgumentException))

(fact "Fundamental frequency of a periodic signal"
      (let [sample-rate 8000
            frequency 200
            signal (mapv
                     (fn [i]
                       (Math/sin
                         (* 2 Math/PI frequency
                            (/ i (double sample-rate)))))
                     (range 800))]

        (nv/fundamental-frequency signal sample-rate)
        => (roughly 200.0 2.0)))


(fact "Fundamental frequency of a silent signal"
      (nv/fundamental-frequency [0.0 0.0 0.0] 8000)
      => nil)


(fact "Fundamental frequency of an empty signal"
      (nv/fundamental-frequency [] 8000)
      => nil)


(fact "Fundamental frequency with invalid sample rate"
      (nv/fundamental-frequency [1 2 3] 0)
      => (throws IllegalArgumentException))

(fact "Fundamental frequency of different signals"
      (let [sample-rate 16000

            generate-signal
            (fn [frequency]
              (mapv
                (fn [i]
                  (Math/sin
                    (* 2 Math/PI frequency
                       (/ i (double sample-rate)))))
                (range 3200)))]

        (nv/fundamental-frequency
          (generate-signal 120) sample-rate)
        => (roughly 120.0 3.0)

        (nv/fundamental-frequency
          (generate-signal 300) sample-rate)
        => (roughly 300.0 5.0)))

(fact "Normalized autocorrelation of a periodic signal"
      (nv/normalized-autocorrelation
        [1 0 -1 0 1 0 -1 0] 4)
      => (roughly 1.0))


(fact "Normalized autocorrelation with zero lag"
      (nv/normalized-autocorrelation [1 2 3] 0)
      => (roughly 1.0))


(fact "Normalized autocorrelation of a silent signal"
      (nv/normalized-autocorrelation [0 0 0] 1)
      => 0.0)


(fact "Normalized autocorrelation of an empty signal"
      (nv/normalized-autocorrelation [] 0)
      => 0.0)


(fact "Normalized autocorrelation with invalid lag"
      (nv/normalized-autocorrelation [1 2 3] -1)
      => (throws IllegalArgumentException))

(fact "F0 estimates of a periodic signal"
      (let [sample-rate 8000
            frequency 200
            signal (mapv
                     (fn [i]
                       (Math/sin
                         (* 2 Math/PI frequency
                            (/ i (double sample-rate)))))
                     (range 1600))

            estimates (nv/f0-estimates signal sample-rate 800)]

        (count estimates) => 2

        (every? #(<= (Math/abs (- % 200.0)) 2.0)
                estimates)
        => true))


(fact "F0 estimates of a silent signal"
      (nv/f0-estimates [0.0 0.0 0.0 0.0] 8000 2)
      => [])

(fact "Fundamental frequency of a noisy signal"
      (let [sample-rate 8000
            frequency 200

            signal (mapv
                     (fn [i]
                       (let [time (/ i (double sample-rate))
                             sine (Math/sin
                                    (* 2 Math/PI frequency time))
                             noise (* 0.1
                                      (Math/sin
                                        (* 2 Math/PI 137 time)))]
                         (+ sine noise)))
                     (range 1600))]

        (nv/fundamental-frequency signal sample-rate)
        => (roughly 200.0 2.0)))

(fact "F0 analysis of a periodic signal"
      (let [sample-rate 8000
            frequency 200
            signal (mapv
                     (fn [i]
                       (Math/sin
                         (* 2 Math/PI frequency
                            (/ i (double sample-rate)))))
                     (range 1600))

            result (nv/analyze-f0 signal sample-rate)]

        (:frequency result) => (roughly 200.0 2.0)
        (:correlation result) => (roughly 1.0 0.01)))


(fact "F0 analysis of a silent signal"
      (nv/analyze-f0 [0.0 0.0 0.0] 8000)
      => nil)


(fact "F0 analysis of an empty signal"
      (nv/analyze-f0 [] 8000)
      => nil)

(fact "F0 analysis of signal segments"
      (let [sample-rate 8000
            frequency 200
            signal (mapv
                     (fn [i]
                       (Math/sin
                         (* 2 Math/PI frequency
                            (/ i (double sample-rate)))))
                     (range 1600))

            results (nv/analyze-f0-segments signal sample-rate 800)]

        (count results) => 2

        (every? #(<= (Math/abs (- (:frequency %) 200.0)) 2.0)
                results)
        => true

        (every? #(> (:correlation %) 0.9)
                results)
        => true))


(fact "F0 analysis of silent segments"
      (nv/analyze-f0-segments [0.0 0.0 0.0 0.0] 8000 2)
      => [])

(fact "F0 analysis preserves segment positions"
      (let [sample-rate 8000
            frequency 200

            tone (mapv
                   (fn [i]
                     (Math/sin
                       (* 2 Math/PI frequency
                          (/ i (double sample-rate)))))
                   (range 800))

            silence (vec (repeat 800 0.0))

            signal (vec (concat tone silence tone))

            results (nv/analyze-f0-segments signal sample-rate 800)]

        (count results) => 2

        (mapv :segment-index results) => [0 2]

        (mapv :time-seconds results)
        => [0.0 0.2]))