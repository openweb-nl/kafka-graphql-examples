(ns nl.openweb.topology.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (clojure.lang Reflector))
  (:gen-class))

(defonce topology
         (-> (io/resource "topology.edn")
             slurp
             edn/read-string))

(defn get-topic
  [topic-name]
  (get topology topic-name))

(defn get-schema
  [schema-key]
  (let [class-name (str "nl.openweb.data." (name schema-key))]
    (Reflector/getStaticField ^String class-name "SCHEMA$")))

(defn get-schemas
  [topic-name]
  (if-let [schema-keys (nth (get-topic topic-name) 2)]
    (map get-schema schema-keys)))
