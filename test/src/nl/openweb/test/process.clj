(ns nl.openweb.test.process
  (:require [nl.openweb.test.resource-tracker :as rt]))

(defonce functions (atom nil))

(def container-names ["db-ch" "db-ge" "command-handler" "kafka-1" "kafka-2" "kafka-3" "graphql-endpoint"])

(defn init
  [docker-url]
  (reset! functions (mapv (fn [name] (rt/init docker-url name)) container-names)))

(defn get-info
  []
  (let [[db-ch db-ge ch k1 k2 k3 ge] (pmap rt/safe-get-usage @functions)]
    [(:cpu-pct db-ch)
     (:mem-mib db-ch)
     (:cpu-pct db-ge)
     (:mem-mib db-ge)
     (:cpu-pct ch)
     (:mem-mib ch)
     (+ (:cpu-pct k1) (:cpu-pct k2) (:cpu-pct k3))
     (+ (:mem-mib k1) (:mem-mib k2) (:mem-mib k3))
     (:cpu-pct ge)
     (:mem-mib ge)]))

(defn close
  []
  (reset! functions nil))
