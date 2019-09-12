(ns nl.openweb.test.load
  (:require [nl.openweb.test.generator :as generator]))

(def latencies (atom []))

(defn add-generator [generator-count] (generator/init "ws://localhost:8888/graphql-ws" generator-count latencies))

(defn get-latencies [] (first (reset-vals! latencies [])))
