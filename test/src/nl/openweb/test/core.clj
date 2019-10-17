(ns nl.openweb.test.core
  (:require [clojure.test :refer :all]
            [nl.openweb.test.analysis :as analysis]
            [nl.openweb.test.load :as load]
            [nl.openweb.test.interactions :as interactions]
            [nl.openweb.test.file :as file]
            [nl.openweb.test.process :as process])
  (:import (java.time Instant))
  (:gen-class))

(def max-interaction-time 5000)
(def min-loop-time 1000)
(def max-loops 6000)
(def max-time-outs 10)
(def seconds-to-generate-generator 60)
(def loops-for-success 1000)

(defn init
  []
  (process/init)
  (file/init)
  (interactions/prep max-interaction-time))

(defn close
  [loop-number]
  (interactions/close)
  (file/close)
  (process/close)
  (if (> loop-number loops-for-success)
    (System/exit 0)
    (System/exit 1)))

(defn optionally-increase-generators
  [generators-count loop-number]
  (if (= 0 (mod loop-number seconds-to-generate-generator))
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
  [loop-number generators-count time-outs interaction-time current-time]
  (if (> max-interaction-time interaction-time)
    (add-row loop-number current-time interaction-time generators-count time-outs)
    (time-out current-time time-outs)))

(defn analytics-loop
  [start loop-number generators-count time-outs]
  (let [interaction-time (interactions/safe-run loop-number)
        current-time (Instant/now)
        new-time-outs (add-row-or-time-out loop-number generators-count time-outs interaction-time current-time)
        millis-till-next (- (+ start (* min-loop-time loop-number)) (inst-ms current-time))]
    (if (pos? millis-till-next) (Thread/sleep millis-till-next))
    (if
      (and
        (> max-time-outs new-time-outs)
        (> max-loops loop-number))
      (recur start (inc loop-number) (optionally-increase-generators generators-count loop-number) new-time-outs)
      loop-number)))

(defn do-tests
  []
  (init)
  (let [loop-number (analytics-loop (inst-ms (Instant/now)) 1 0 0)]
    (close loop-number)))

(defn generate-data-json
  [file-name]
  (let [config (file/get-data file-name)
        data (reduce-kv (fn [i k v] (assoc i k (file/get-data v))) {} (:mapping config))]
    (analysis/process file-name data)))

(defn -main
  [& [file-name]]
  (if (nil? file-name)
    (do-tests)
    (generate-data-json file-name)))
