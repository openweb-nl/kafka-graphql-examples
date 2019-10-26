(ns nl.openweb.test.generator
  (:require [clojure.tools.logging :as log]
            [nl.openweb.test.graphql-client :as gqlc]
            [clojure.string :as str])
  (:import (java.util UUID)))

(def min-time-between-subscriptions 250)

(defn open-account
  [ws-connection id]
  (gqlc/subscribe
    ws-connection id
    "($username: String! $password: String!){get_account(username: $username password: $password) {iban token}}"
    {:username id :password id}))

(defn money-transfer
  [ws-connection sequence-number account-first account-second [type amount]]
  (let [username (if (= type :to) (:username account-second) (:username account-first))
        from (if (= type :to) (:iban account-second) (:iban account-first))
        token (if (= type :to)
                (:token account-second)
                (if (= type :invalid_token) "bogus-token" (:token account-first)))
        to (if (= type :to) (:iban account-first) (:iban account-second))]
    (gqlc/subscribe
      ws-connection (str "transfer-" (name type) "|" sequence-number "|" amount "|" (System/currentTimeMillis))
      "($amount: Int! $uuid: String! $username: String! $to: String! $token: String! $from: String! $descr: String!){
       money_transfer(amount: $amount uuid: $uuid username: $username to: $to token: $token from: $from descr: $descr)
       {reason success uuid}}"
      {:amount   amount
       :username username
       :from     from
       :token    token
       :to       to
       :descr    (print-str amount from "to" to)
       :uuid     (str (UUID/randomUUID))})))

(defn add-account
  [state data type name]
  (let [account (-> data
                    :get_account
                    (assoc :username name))]
    (swap! state assoc type account)))

(defn next-type-and-amount
  [transfer-type amount]
  (let [int-amount (read-string amount)]
    (condp = transfer-type
      :from (if (>= int-amount 5000) [:invalid_token 100] [:to (+ 100 int-amount)])
      :to (if (>= int-amount 5000) [:insufficient_funds 100000] [:from (+ 100 int-amount)])
      :invalid_token [:from 100]
      :insufficient_funds [:to 100])))

(defn check-and-next
  [id data ws-connection latencies account-first account-second]
  (let [{:keys [success reason]} (:money_transfer data)
        [type sequence-number amount time] (str/split id #"\|")
        transfer-type (keyword (second (str/split type #"-")))
        latency (- (System/currentTimeMillis) (read-string time))]
    (swap! latencies conj latency)
    (gqlc/unsubscribe ws-connection id)
    (when (< latency min-time-between-subscriptions) (Thread/sleep (- min-time-between-subscriptions latency)))
    (condp = transfer-type
      :from (when (not success) (throw (Exception. "from transfer failed")))
      :to (when (not success) (throw (Exception. "to transfer failed")))
      :invalid_token (when (not= reason "invalid token") (throw (Exception. "invalid token transfer failed")))
      :insufficient_funds (when (not= reason "insufficient funds") (throw (Exception. "insufficient funds transfer failed"))))
    (money-transfer ws-connection sequence-number account-first account-second (next-type-and-amount transfer-type amount))))

(defn handle-account-add
  [ws-connection id state data]
  (let [type (keyword (first (str/split id #"\|")))]
    (gqlc/unsubscribe ws-connection id)
    (add-account state data type id)
    (if
      (= type :account-first)
      (open-account ws-connection (str/replace id #"first" "second"))
      (money-transfer ws-connection (second (str/split id #"\|")) (:account-first @state) (:get_account data) [:from 100]))))

(defn- r-starts-with?
  [^String substr ^CharSequence s]
  (str/starts-with? s substr))

(defn on-data
  [state]
  (fn [id data]
    (let [{:keys [ws-connection latencies account-first account-second]} @state]
      (condp r-starts-with? id
        "account-"
        (handle-account-add ws-connection id state data)
        "transfer-"
        (check-and-next id data ws-connection latencies account-first account-second)
        (log/debug "Ignoring data " id " - " data)))))

(defn init
  [ws-url sequence-number latencies]
  (let [state (atom {:latencies latencies})
        ws-connection (gqlc/connect ws-url (on-data state))]
    (swap! state assoc :ws-connection ws-connection)
    (open-account ws-connection (str "account-first|" sequence-number))))
