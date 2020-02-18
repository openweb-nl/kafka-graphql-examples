(ns nl.openweb.test.core
  (:require [nl.openweb.test.analysis :as analysis]
            [nl.openweb.test.load :as load]
            [nl.openweb.test.interactions :as interactions]
            [nl.openweb.test.file :as file]
            [nl.openweb.test.process :as process])
  (:import (java.time Instant))
  (:gen-class))

(defn init
  [config]
  (process/init (:docker-url config))
  (file/init (:base-test-file-name config))
  (interactions/prep (:max-interaction-time config)))

(defn close
  [config loop-number]
  (interactions/close)
  (file/close)
  (process/close)
  (if (>= loop-number (:loops-for-success config))
    (System/exit 0)
    (System/exit 1)))

(defn optionally-increase-generators
  [config generators-count loop-number]
  (if (= 0 (mod loop-number (:loops-to-generate-generator config)))
    (let [new-generators (inc generators-count)]
      (load/add-generator generators-count)
      (println "added generator, total is now" new-generators)
      new-generators)
    generators-count))

(defn add-row
  [loop-number current-time interaction-time generators-count time-outs]
  (file/add-row loop-number current-time interaction-time generators-count)
  time-outs)

(defn time-out
  [current-time time-outs]
  (let [new-time-outs (inc time-outs)]
    (println "timeout" new-time-outs "occurred at" (str current-time))
    new-time-outs))

(defn add-row-or-time-out
  [config loop-number generators-count time-outs interaction-time current-time]
  (if (> (:max-interaction-time config) interaction-time)
    (add-row loop-number current-time interaction-time generators-count time-outs)
    (time-out current-time time-outs)))

(defn maybe-sleep
  [current-time config start loop-number]
  (let [continue-time-ms (+ start (* (:min-loop-time config) loop-number))
        current-time-ms (inst-ms current-time)]
    (when
      (> continue-time-ms current-time-ms)
      (Thread/sleep (- continue-time-ms current-time-ms)))))

(defn analytics-loop
  [config start loop-number generators-count time-outs]
  (let [interaction-time (interactions/safe-run loop-number (:interaction-interval config))
        current-time (Instant/now)
        new-time-outs (add-row-or-time-out config loop-number generators-count time-outs interaction-time current-time)]
    (maybe-sleep current-time config start loop-number)
    (if
      (or
        (> new-time-outs (:max-time-outs config))
        (>= loop-number (:max-loops config)))
      loop-number
      (recur config start (inc loop-number) (optionally-increase-generators config generators-count loop-number) new-time-outs))))

(defn do-tests
  [config]
  (init config)
  (let [loop-number (analytics-loop config (inst-ms (Instant/now)) 1 0 0)]
    (close config loop-number)))

(defn generate-data-json
  [file-name config]
  (let [data (reduce-kv (fn [i k v] (assoc i k (file/get-data v))) {} (:mapping config))]
    (analysis/process file-name data)))

(defn -main
  [file-name]
  (let [config (file/get-data file-name)]
    (if (:base-test-file-name config)
      (do-tests config)
      (generate-data-json file-name config))))
