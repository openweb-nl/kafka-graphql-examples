(ns nl.openweb.test.interactions
  (:require [etaoin.api :as a]
            [etaoin.keys :as k])
  (:import (java.time Instant)))

(defonce driver (atom nil))
(def deposit-keys [:deposit-1000 :deposit-2000 :deposit-5000 :deposit-10000 :deposit-20000])
(def deposit-values ["10,00" "20,00" "50,00" "100,00" "200,00"])
(def transaction-keys ["11,11" "23,23" "34,89" "99,99" "210,78"])
(def transaction-descriptions ["drinks" "parking" "groceries" "present" "fine"])
(def transaction-values ["11,11" "23,23" "34,89" "99,99" "210,78"])
(def time-out (atom 5))

(defn wait-till-value
  [expected-value interaction-interval]
  (let [start (inst-ms (Instant/now))]
    (a/wait-has-text
      @driver
      {:css "#transactions div:nth-child(1) div div:nth-child(1) p:nth-child(1) span:nth-child(2)"}
      expected-value
      {:interval interaction-interval :timeout @time-out})
    (- (inst-ms (Instant/now)) start)))

(defn run-deposit
  [m]
  (a/click @driver (nth deposit-keys m))
  (nth deposit-values m))

(defn run-transaction
  [m]
  (let [k (nth transaction-keys m)
        d (nth transaction-descriptions m)]
    (doto @driver
      (a/fill {:css "#transfer-form div:nth-child(1) input"} k k/enter)
      (a/fill {:css "#transfer-form div:nth-child(2) input"} "NL66OPEN0000000000" k/enter)
      (a/fill {:css "#transfer-form div:nth-child(3) input"} d k/enter)
      (a/click {:css "#transfer-form div:nth-child(4) div a"}))
    (nth transaction-values m)))

(defn run
  [loop-number interaction-interval]
  (let [m (mod loop-number 10)
        v (if (< m 5)
            (run-deposit m)
            (run-transaction (- m 5)))]
    (wait-till-value v interaction-interval)))

(defn safe-run
  [loop-number interaction-interval]
  (try
    (run loop-number interaction-interval)
    (catch Exception error
      (println "An error occurred" (.toString error))
      Integer/MAX_VALUE)))

(defn wait-till-button
  []
  (let [start (inst-ms (Instant/now))]
    (a/wait-exists @driver :deposit-1000 {:interval 0.2 :timeout 30})
    (println "waited for" (- (inst-ms (Instant/now)) start) "ms to log in and see the deposit 10 button")))

(defn login
  []
  (doto @driver
    (a/set-window-size 1920 1080)
    (a/go "http://localhost:8181/")
    (a/wait 2)
    (a/click {:css "#flex-main-menu a:nth-child(2)"})
    (a/wait 2)
    (a/fill {:css "#login-form div:nth-child(1) input"} "testuser" k/enter)
    (a/fill {:css "#login-form div:nth-child(2) input"} "password" k/enter)
    (a/wait 1)
    (a/click {:css "#login-form div:nth-child(3)"})))

(defn prep
  [max-interaction-time]
  (reset! time-out (int (Math/ceil (/ max-interaction-time 1000))))
  (reset! driver (a/chrome-headless))
  (login)
  (wait-till-button))

(defn close
  []
  (a/delete-session @driver))
