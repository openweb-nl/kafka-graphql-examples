(defproject nl.openweb/test "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[cheshire "5.10.0"]
                 [com.fasterxml.jackson.core/jackson-annotations :version]
                 [com.fasterxml.jackson.core/jackson-core :version]
                 [com.fasterxml.jackson.core/jackson-databind :version]
                 [etaoin "0.3.6"]
                 [kixi/stats "0.5.2"]
                 [lispyclouds/clj-docker-client "0.5.1" :exclusions [org.jetbrains.kotlin/kotlin-stdlib-common]]
                 [org.apache.httpcomponents/httpasyncclient "4.1.4"]
                 [org.apache.httpcomponents/httpclient-cache "4.5.11" :exclusions [org.apache.httpcomponents/httpcore org.apache.httpcomponents/httpclient commons-codec]]
                 [org.clojure/clojure :version]
                 [org.clojure/data.json :version]
                 [org.clojure/tools.logging :version]
                 [stylefruits/gniazdo "1.1.3"]]
  :main nl.openweb.test.core
  :profiles {:uberjar {:omit-source  true
                       :aot          :all
                       :uberjar-name "test.jar"}})
