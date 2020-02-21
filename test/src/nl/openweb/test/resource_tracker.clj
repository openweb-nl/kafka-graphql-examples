(ns nl.openweb.test.resource-tracker
  (:require [clj-docker-client.core :as docker]))

(def bytes-in-mb (* 1024. 1024.))

(defn init
  [uri container-name]
  (let [docker-conn (docker/connect {:uri             uri
                                     :connect-timeout 1000
                                     :read-timeout    5000
                                     :write-timeout   5000
                                     :call-timeout    30000})
        containers (docker/client {:category :containers :conn docker-conn})
        stats-f (fn [] (docker/invoke containers {:op :ContainerStats :params {:id container-name :stream false}}))]
    (stats-f)
    stats-f))

(defn- get-cpu-pct
  [stats]
  (let [old (:precpu_stats stats)
        new (:cpu_stats stats)
        container-use (- (get-in new [:cpu_usage :total_usage]) (get-in old [:cpu_usage :total_usage]))
        system-use (- (:system_cpu_usage new) (:system_cpu_usage old))]
    (double (* 100 (/ container-use system-use)))))

(defn- get-mem-mib
  [stats]
  (double (/
            (- (get-in stats [:memory_stats :usage])
               (get-in stats [:memory_stats :stats :total_cache]))
            bytes-in-mb)))

(defn- get-usage
  [stats-f]
  (let [stats (stats-f)
        cpu-pct (get-cpu-pct stats)
        mem-mib (get-mem-mib stats)]
    {:cpu-pct cpu-pct :mem-mib mem-mib}))

(defn safe-get-usage
  [stats-f]
  (try
    (get-usage stats-f)
    (catch Exception error
      (println "An error occurred" (.toString error))
      {})))
