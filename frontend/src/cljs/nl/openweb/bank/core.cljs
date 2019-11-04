(ns nl.openweb.bank.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-graph.core :as re-graph]
            [nl.openweb.bank.config :as config]
            [nl.openweb.bank.db :refer [default-db]]
            [nl.openweb.bank.transactions :refer [get-dispatches]]
            [nl.openweb.bank.events :as events]
            [nl.openweb.bank.routes :as routes]
            [nl.openweb.bank.views :as views]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch [::re-graph/init {:ws-url   "ws://localhost:8888/graphql-ws"
                                       :http-url "http://localhost:8888/graphql"}])
  (doseq [dispatch (get-dispatches default-db)]
    (re-frame/dispatch dispatch))
  (dev-setup)
  (mount-root))
