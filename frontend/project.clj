(defproject nl.openweb/bank "0.1.0-SNAPSHOT"
  :description "front-end for the kafka workshop"
  :url "https://github.com/openweb-nl/kafka-graphql-examples/tree/master/frontend"
  :dependencies [[cljsjs/vega "5.9.0-0" :exclusions [com.google.errorprone/error_prone_annotations com.google.code.findbugs/jsr305]]
                 [cljsjs/vega-lite "4.0.2-0" :exclusions [com.google.errorprone/error_prone_annotations com.google.code.findbugs/jsr305]]
                 [cljsjs/vega-embed "6.0.0-0" :exclusions [com.google.errorprone/error_prone_annotations com.google.code.findbugs/jsr305]]
                 [cljsjs/vega-tooltip "0.20.0-0" :exclusions [com.google.errorprone/error_prone_annotations com.google.code.findbugs/jsr305]]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597" :exclusions [com.google.errorprone/error_prone_annotations com.google.code.findbugs/jsr305]]
                 [reagent "0.9.1"]
                 [re-frame "0.11.0"]
                 [re-graph "0.1.11" :exclusions [args4j]]
                 [bidi "2.1.6"]
                 [kibu/pushy "0.3.8"]]
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-sass "0.5.0"]]
  :min-lein-version "2.5.3"
  :source-paths ["src/clj"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "resources/public/css"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :sass {:src              "resources/app/stylesheets"
         :output-directory "resources/public/css"
         :source-maps      false
         :command          :sassc}
  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "1.0.0"]
                   [day8.re-frame/re-frame-10x "0.4.7"]]
    :plugins      [[lein-figwheel "0.5.19"]]}}
  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "nl.openweb.bank.core/mount-root"}
     :compiler     {:main                 nl.openweb.bank.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :optimizations        :none
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            nl.openweb.bank.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
