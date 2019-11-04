(ns nl.openweb.bank.routes
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]
            [nl.openweb.bank.events :as events]))

(def routes ["/" {""                                  :home
                  "employee"                          :bank-employee
                  "client"                            :client
                  ["results/" :category "/" :x-value] :results}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn- dispatch-route [matched-route]
  (let [handler (:handler matched-route)
        params (:route-params matched-route)]
    (re-frame/dispatch [::events/set-selected-nav handler])
    (if-let [category (:category params)]
      (re-frame/dispatch [::events/set-category (keyword category)]))
    (if-let [x-value (:x-value params)]
      (re-frame/dispatch [::events/set-x-value (keyword x-value)]))))

(defn app-routes []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))
