(defproject nl.openweb/open-bank-mark "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:inherited
                      {:repositories  [["confluent" "https://packages.confluent.io/maven/"]]
                       :aliases       {"all" ^:displace ["do" "clean," "test," "install", "uberjar"]
                                       "-f"  ["with-profile" "+fast"]}
                       :scm           {:dir ".."}
                       :javac-options ["-target" "11" "-source" "11"]
                       :license       {:name "MIT License"
                                       :url  "https://opensource.org/licenses/MIT"
                                       :key  "mit"
                                       :year 2019}}
            :versions {ch.qos.logback/logback-classic                 "1.3.0-alpha5"
                       com.damballa/abracad                           "0.4.14-alpha2"
                       com.fasterxml.jackson.core/jackson-annotations "2.10.0"
                       com.fasterxml.jackson.core/jackson-core        "2.10.0"
                       com.fasterxml.jackson.core/jackson-databind    "2.10.0"
                       hikari-cp/hikari-cp                            "2.9.0"
                       io.confluent/kafka-avro-serializer             "5.3.1"
                       org.apache.avro/avro                           "1.9.1"
                       org.clojure/clojure                            "1.10.1"
                       org.clojure/data.json                          "0.2.6"
                       org.clojure/tools.logging                      "0.5.0"
                       org.postgresql/postgresql                      "42.2.8"
                       seancorfield/next.jdbc                         "1.0.9"}})
