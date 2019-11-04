(ns nl.openweb.command-handler.db
  (:require
    [hikari-cp.core :as h]
    [nl.openweb.topology.value-generator :as vg]
    [next.jdbc :as j]
    [next.jdbc.sql :as sql])
  (:import (nl.openweb.data ConfirmMoneyTransfer)
           (java.util UUID)))

(def db-port (read-string (or (System/getenv "DB_PORT") "5432")))
(def db-hostname (or (System/getenv "DB_HOSTNAME") "localhost"))
(def db-password (or (System/getenv "DB_PASSWORD") "kafka-graphql-pw"))

(defn datasource-options
  [db-port db-hostname db-password]
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  10
   :pool-name          "db-pool"
   :adapter            "postgresql"
   :username           "clojure_ch"
   :password           db-password
   :database-name      "balancedb"
   :server-name        db-hostname
   :port-number        db-port
   :register-mbeans    false})

(defonce datasource (atom nil))

(defn init
  []
  (reset! datasource (h/make-datasource (datasource-options db-port db-hostname db-password))))

(defn deref-first
  [refs]
  (if-let [f (first refs)]
    (do
      @f
      (rest refs))))

(defn find-balance-by-iban
  [iban]
  (when
    (vg/valid-open-iban iban)
    (with-open [conn (j/get-connection @datasource)]
      (j/execute-one! conn ["SELECT * FROM balance WHERE iban = ?" iban]))))

(defn transfer-from!
  [iban token amount]
  (when
    (vg/valid-open-iban iban)
    (with-open [conn (j/get-connection @datasource)]
      (j/execute-one! conn ["
      UPDATE balance
      SET amount =
        CASE WHEN balance.token != ? THEN balance.amount
        WHEN balance.amount - ? >= balance.lmt THEN balance.amount - ?
        ELSE balance.amount END FROM balance AS old_balance
      WHERE balance.iban = ? AND balance.balance_id = old_balance.balance_id
      RETURNING balance.*, old_balance.amount as old_amount"
                            token amount amount iban]))))

(defn transfer-to!
  [iban amount]
  (when
    (vg/valid-open-iban iban)
    (with-open [conn (j/get-connection @datasource)]
      (j/execute-one! conn ["UPDATE balance SET amount = balance.amount + ? WHERE balance.iban = ? RETURNING balance.*"
                            amount iban]))))

(defn insert-balance!
  [mp]
  (let [cmp (-> mp
                (dissoc :uuid)
                (dissoc :reason))]
    (with-open [conn (j/get-connection @datasource)]
      (sql/insert! conn :balance cmp))))

(defn find-cac-by-uuid
  [uuid]
  (with-open [conn (j/get-connection @datasource)]
    (j/execute-one! conn ["SELECT * FROM cac WHERE uuid = ?" uuid])))

(defn insert-cac!
  [mp]
  (with-open [conn (j/get-connection @datasource)]
    (sql/insert! conn :cac mp)))

(defn find-cmt-by-uuid
  [uuid]
  (with-open [conn (j/get-connection @datasource)]
    (j/execute-one! conn ["SELECT * FROM cmt WHERE uuid = ?" uuid])))

(defn insert-cmt!
  [mp]
  (with-open [conn (j/get-connection @datasource)]
    (sql/insert! conn :cmt mp)))

(defn get-account
  [uuid]
  (if-let [result (find-cac-by-uuid uuid)]
    {:uuid   (:cac/uuid result)
     :iban   (:cac/iban result)
     :token  (:cac/token result)
     :reason (:cac/reason result)}
    (let [iban (vg/new-iban)
          reason (when (find-balance-by-iban iban) "generated iban already exists, try again")
          mp {:uuid uuid :iban iban :token (vg/new-token) :reason reason}]
      (insert-cac! mp)
      (when (nil? (:reason mp)) (insert-balance! mp))
      mp)))

(defn transfer-update!
  [uuid ^ConfirmMoneyTransfer tm]
  (if-let [from-map (transfer-from! (.getFrom tm) (.getToken tm) (.getAmount tm))]
    (cond
      (not (= (:balance/token from-map) (.getToken tm))) {:uuid uuid :reason "invalid token"}
      (= (:balance/amount from-map) (:balance/old_amount from-map)) {:uuid uuid :reason "insufficient funds"}
      :else (if-let [to-map (transfer-to! (.getTo tm) (.getAmount tm))]
              {:from from-map :to to-map}
              {:from from-map}))
    (if-let [to-map (transfer-to! (.getTo tm) (.getAmount tm))]
      {:to to-map}
      {:uuid uuid :reason "both to and from not known at this bank"})))

(defn invalid-from
  [from]
  (if (= from "cash")
    false
    (not (vg/valid-open-iban from))))

(defn do-transfer!
  [uuid ^ConfirmMoneyTransfer tm]
  (let [result (cond
                 (invalid-from (.getFrom tm)) {:uuid uuid :reason (str "from is invalid")}
                 (= (.getFrom tm) (.getTo tm)) {:uuid uuid :reason "from and to can't be same for transfer"}
                 :else (transfer-update! uuid tm))]
    (if
      (:reason result)
      (insert-cmt! result)
      (insert-cmt! {:uuid uuid}))
    result))

(defn transfer
  [^ConfirmMoneyTransfer tm]
  (let [uuid (-> (.getId tm)
                 .bytes
                 vg/bytes->uuid)]
    (if-let [result (find-cmt-by-uuid uuid)]
      {:uuid   (:cmt/uuid result)
       :reason (:cmt/reason result)}
      (do-transfer! uuid tm))))
