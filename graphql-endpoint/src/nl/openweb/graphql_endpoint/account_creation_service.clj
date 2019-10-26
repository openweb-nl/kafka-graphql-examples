(ns nl.openweb.graphql-endpoint.account-creation-service
  (:require [com.stuartsierra.component :as component]
            [crypto.password.pbkdf2 :as crypto]
            [nl.openweb.topology.clients :as clients]
            [nl.openweb.topology.value-generator :refer [bytes->uuid uuid->bytes]]
            [next.jdbc :as j]
            [next.jdbc.sql :as sql])
  (:import (org.apache.kafka.clients.consumer ConsumerRecord)
           (nl.openweb.data AccountCreationConfirmed AccountCreationFailed ConfirmAccountCreation Uuid)
           (java.util UUID)))

(def command-topic (or (System/getenv "KAFKA_COMMAND_TOPIC") "commands"))
(def acf-topic (or (System/getenv "KAFKA_ACF_TOPIC") "account_creation_feedback"))
(def client-id (or (System/getenv "KAFKA_CLIENT_ID") "graphql-endpoint-accounts"))

(defn v->map
  [v]
  (cond
    (instance? AccountCreationConfirmed v)
    {:iban   (.getIban v)
     :token  (.getToken v)
     :reason nil}
    (instance? AccountCreationFailed v)
    {:iban   nil
     :token  nil
     :reason (.getReason v)}))

(defn remove-account!
  [datasource uuid]
  (with-open [conn (j/get-connection datasource)]
    (sql/delete! conn :account ["uuid = ?" uuid])))

(defn handle-reply
  [^ConsumerRecord cr datasource subscriptions]
  (let [v (.value cr)
        uuid (bytes->uuid (.bytes (.getId v)))
        v-map (v->map v)]
    (when (:reason v-map)
      (remove-account! datasource uuid))
    (doseq [[s-id source-stream] (vals (:map @subscriptions))]
      (when (= s-id uuid)
        (source-stream v-map)))))

(defn add-stream
  [subscriptions uuid source-stream]
  (let [new-id (inc (:id subscriptions))]
    (-> subscriptions
        (assoc :id new-id)
        (assoc-in [:map new-id] [uuid source-stream]))))

(defn add-sub
  [subscriptions uuid source-stream]
  (let [new-subscriptions (swap! subscriptions add-stream uuid source-stream)]
    (:id new-subscriptions)))

(defn find-account-by-username
  [db username]
  (with-open [conn (j/get-connection (get-in db [:db :datasource]))]
    (j/execute-one! conn ["SELECT * FROM account WHERE username = ?" username])))

(defn insert-account!
  [datasource username password uuid]
  (with-open [conn (j/get-connection datasource)]
    (sql/insert! conn :account {:username username
                                :password (crypto/encrypt password)
                                :uuid     uuid})))

(defn get-account
  [db source-stream username password]
  (if-let [account (find-account-by-username db username)]
    (if (crypto/check password (:account/password account))
      (do
        (add-sub (:subscriptions db) (:account/uuid account) source-stream)
        (clients/produce (get-in db [:kafka-producer :producer]) command-topic username (ConfirmAccountCreation. (Uuid. (uuid->bytes (:account/uuid account))))))
      (source-stream {:iban   nil
                      :token  nil
                      :reason "incorrect password"}))
    (let [uuid (UUID/randomUUID)
          sub-id (add-sub (:subscriptions db) uuid source-stream)]
      (insert-account! (get-in db [:db :datasource]) username password uuid)
      (clients/produce (get-in db [:kafka-producer :producer]) command-topic username (ConfirmAccountCreation. (Uuid. (uuid->bytes uuid))))
      sub-id)))

(defrecord AccountCreationService []

  component/Lifecycle

  (start [this]
    (let [subscriptions (atom {:id 0 :map {}})
          stop-consume-f (clients/consume client-id client-id acf-topic #(handle-reply % (get-in this [:db :datasource]) subscriptions))]
      (-> this
          (assoc :subscriptions subscriptions)
          (assoc :stop-consume stop-consume-f))))

  (stop [this]
    ((:stop-consume this))
    (doseq [[_ source-stream] (vals (:map @(:subscriptions this)))]
      (source-stream nil))
    (-> this
        (assoc :subscriptions nil)
        (assoc :stop-consume nil))))

(defn new-service
  []
  {:account-creation-service (-> {}
                  map->AccountCreationService
                  (component/using [:kafka-producer :db]))})

(defn stop-transaction-subscription
  [db id]
  (let [source-stream (second (get (:map @(:subscriptions db)) id))]
    (when source-stream
      (source-stream nil)
      (swap! (:subscriptions db) #(update % :map dissoc id)))))
