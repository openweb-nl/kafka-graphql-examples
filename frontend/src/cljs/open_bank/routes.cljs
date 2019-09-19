(ns open-bank.routes
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as re-frame]
            [open-bank.events :as events]))

(def routes ["/" {""           :home
                  "employee"   :bank-employee
                  "client"     :client
                  "background" :background}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn- dispatch-route [matched-route]
  (re-frame/dispatch [::events/set-selected-nav (:handler matched-route)]))

(defn app-routes []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))