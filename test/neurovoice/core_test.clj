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