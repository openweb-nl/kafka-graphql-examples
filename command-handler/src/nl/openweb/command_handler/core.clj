(ns nl.openweb.command-handler.core
  (:require [nl.openweb.command-handler.db :as db]
            [nl.openweb.topology.clients :as clients]
            [nl.openweb.topology.value-generator :as vg])
  (:import (nl.openweb.data ConfirmAccountCreation AccountCreationConfirmed AccountCreationFailed MoneyTransferFailed ConfirmMoneyTransfer MoneyTransferConfirmed BalanceChanged)
           (org.apache.avro.specific SpecificRecordBase)
           (org.apache.kafka.clients.consumer ConsumerRecord))
  (:gen-class))

(def command-topic (or (System/getenv "KAFKA_COMMAND_TOPIC") "commands"))
(def acf-topic (or (System/getenv "KAFKA_ACF_TOPIC") "account_creation_feedback"))
(def mtf-topic (or (System/getenv "KAFKA_MTF_TOPIC") "money_transfer_feedback"))
(def bc-topic (or (System/getenv "KAFKA_BC_TOPIC") "balance_changed"))
(def client-id (or (System/getenv "KAFKA_CLIENT_ID") "command-handler"))

(defn handle-cac
  [producer key ^ConfirmAccountCreation value]
  (let [result (db/get-account (-> (.getId value)
                                   .bytes
                                   vg/bytes->uuid) (.toString (.getAType value)))
        feedback (if (:reason result)
                   (AccountCreationFailed. (.getId value) (:reason result))
                   (AccountCreationConfirmed. (.getId value) (:iban result) (:token result) (.getAType value)))]
    (clients/produce producer acf-topic feedback)))

(defn ->bc
  [from ^ConfirmMoneyTransfer cmt balance-row]
  (BalanceChanged.
    (:balance/iban balance-row)
    (:balance/amount balance-row)
    (if from (- (.getAmount cmt)) (.getAmount cmt))
    (if from (.getTo cmt) (.getFrom cmt))
    (.getDescription cmt)))

(defn handle-cmt
  [producer key ^ConfirmMoneyTransfer value]
  (let [result (db/transfer value)]
    (if-let [from (:from result)]
      (clients/produce producer bc-topic (->bc true value from)))
    (if-let [to (:to result)]
      (clients/produce producer bc-topic (->bc false value to)))
    (if-let [reason (:reason result)]
      (clients/produce producer mtf-topic (MoneyTransferFailed. (.getId value) reason))
      (clients/produce producer mtf-topic (MoneyTransferConfirmed. (.getId value)))
      )))

(defn handle-all
  [producer]
  (fn [^ConsumerRecord record]
    (let [key (.key record)
          value (.value record)]
      (condp instance? value
        ConfirmAccountCreation (handle-cac producer key value)
        ConfirmMoneyTransfer (handle-cmt producer key value))
      )))

(defn -main
  []
  (db/init)
  (let [producer (clients/get-producer client-id)]
    (clients/consume client-id client-id command-topic (handle-all producer))))

