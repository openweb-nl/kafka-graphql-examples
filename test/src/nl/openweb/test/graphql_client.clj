(ns nl.openweb.test.graphql-client
  (:require [cheshire.core :as json]
            [gniazdo.core :as ws]
            [clojure.tools.logging :as log]))

(defn- on-ws-message [on-data-f]
  (fn [m]
    (let [{:keys [type id payload]} (json/decode m keyword)]
      (condp = type
        "data"
        (on-data-f id (:data payload))
        "complete"
        (log/info "Completed:" id payload)
        "error"
        (log/error "Error:" id payload)
        (log/debug "Ignoring graphql-ws event " payload " - " type " - " id)))))

(defn- on-close [] (fn [x y] (log/info "GraphQL websocket closed" x y)))

(defn- on-error [] (fn [e] (log/warn "GraphQL websocket error" e)))

(defn connect
  [ws-url on-data-f]
  (ws/connect ws-url
              :on-receive (on-ws-message on-data-f)
              :on-close (on-close)
              :on-error (on-error)
              :subprotocols ["graphql-ws"]))

(defn- send-ws
  [ws-connection payload]
  (ws/send-msg ws-connection (json/encode payload)))

(defn subscribe
  [ws-connection subscription-id query variables]
  (send-ws ws-connection {:id      subscription-id
                          :type    "start"
                          :payload {:query     (str "subscription " query)
                                    :variables variables}}))

(defn unsubscribe [ws-connection id] (send-ws ws-connection {:id id :type "stop"}))

(defn close [ws-connection] (ws/close ws-connection))