(ns nl.openweb.bank.templates
  (:require [clojure.string :as string]
            [nl.openweb.bank.events :as events]
            [nl.openweb.bank.results :as results]
            [nl.openweb.bank.routes :as routes]
            [re-frame.core :as re-frame]
            [re-graph.core :as re-graph]))

(defn navbar-item
  [nav selected-nav results]
  [:a.navbar-item
   {:class (when (= selected-nav nav) "is-active")
    :href  (if
             (= nav :results)
             (routes/url-for nav :category (name (:category results)) :x-value (name (:x-value results)))
             (routes/url-for nav))
    :key   nav} (string/capitalize (name nav))])

(defn nav-bar
  [selected-nav expand show-left results]
  [:nav#nav-bar.navbar.is-fixed-top {:role "navigation" :aria-label "main navigation"}
   [:div.navbar-brand
    [:a.navbar-item
     {:class (when (= selected-nav :home) "is-active")
      :href  (routes/url-for :home)}
     [:img {:src "/img/logo.svg" :style {:width "140px"}}]]
    [:a.navbar-item.is-hidden-tablet
     {:on-click #(re-frame/dispatch [::events/toggle-show-left])}
     (if show-left
       [:span.icon {:style {:color "#95c23d"}} [:i.mdi.mdi-24px.mdi-arrow-up-drop-circle-outline]]
       [:span.icon {:style {:color "#95c23d"}} [:i.mdi.mdi-24px.mdi-arrow-down-drop-circle-outline]])]
    [:a.navbar-item.is-hidden-desktop
     {:target "_blank", :href "https://www.openweb.nl"}
     [:span.icon {:style {:color "#95c23d"}} [:i.mdi.mdi-24px.mdi-domain]]]
    [:a.navbar-item.is-hidden-desktop
     {:target "_blank", :href "https://twitter.com/OpenWebNL"}
     [:span.icon {:style {:color "#1da1f2"}} [:i.mdi.mdi-24px.mdi-twitter]]]
    [:a.navbar-item.is-hidden-desktop
     {:target "_blank", :href "https://www.linkedin.com/company/open-web-it-services/"}
     [:span.icon {:style {:color "#0077B5"}} [:i.mdi.mdi-24px.mdi-linkedin-box]]]
    [:a.navbar-item.is-hidden-desktop
     {:target "_blank", :href "https://github.com/openweb-nl/kafka-graphql-examples"}
     [:span.icon {:style {:color "#24292e"}} [:i.mdi.mdi-24px.mdi-github-circle]]]
    [:button.button.navbar-burger
     {:on-click #(re-frame/dispatch [::events/toggle-mob-expand])
      :class    (when expand "is-active")}
     [:span]
     [:span]
     [:span]]]
   [:div#main-menu.navbar-menu
    {:class (when expand "is-active")}
    [:div#flex-main-menu.navbar-start
     (map #(navbar-item % selected-nav results) [:bank-employee :client :results])]
    [:div.navbar-end
     [:a.navbar-item.is-hidden-touch
      {:target "_blank", :href "https://www.openweb.nl"}
      [:span.icon {:style {:color "#95c23d"}} [:i.mdi.mdi-24px.mdi-domain]]]
     [:a.navbar-item.is-hidden-touch
      {:target "_blank", :href "https://twitter.com/OpenWebNL"}
      [:span.icon {:style {:color "#1da1f2"}} [:i.mdi.mdi-24px.mdi-twitter]]]
     [:a.navbar-item.is-hidden-touch
      {:target "_blank", :href "https://www.linkedin.com/company/open-web-it-services/"}
      [:span.icon {:style {:color "#0077B5"}} [:i.mdi.mdi-24px.mdi-linkedin-box]]]
     [:a.navbar-item.is-hidden-touch
      {:target "_blank", :href "https://github.com/openweb-nl/kafka-graphql-examples"}
      [:span.icon {:style {:color "#24292e"}} [:i.mdi.mdi-24px.mdi-github-circle]]]]]])

(defn deposit-button
  [iban username amount]
  (let [uuid (str (random-uuid))]
    [:p
     {:key amount}
     [:a.button.is-info.is-fullwidth.is-rounded.is-danger
      {:id       (str "deposit-" amount)
       :on-click #(re-frame/dispatch [::re-graph/subscribe
                                      :ss
                                      (keyword (str "deposit-" uuid))
                                      "($amount: Int! $uuid: String! $username: String! $iban: String!){
                                            money_transfer(amount: $amount username: $username  descr: \"deposit by re-graph\"
                                           from: \"cash\" token: \"cash\" to: $iban uuid: $uuid)
                                           {reason success uuid}}"
                                      {:amount   amount
                                       :username username
                                       :iban     iban
                                       :uuid     uuid}
                                      [::events/on-deposit]])}
      (str "deposit " (/ amount 100) " euro")]]))


(defn client-notification
  [login-status]
  (if (:iban login-status)
    [:div.content
     (for [amount [1000 2000 5000 10000 20000 50000]]
       (deposit-button (:iban login-status) (:username login-status) amount))]
    [:p.notification.is-primary "Login to create/access your account"]))

(defn iban-button
  [transaction employee-iban]
  (if-let [iban (:iban transaction)]
    (if (= employee-iban iban)
      [:p
       {:key iban}
       [:a.button.is-info.is-fullwidth.is-active iban]]
      [:p
       {:key iban}
       [:a.button.is-primary.is-fullwidth
        {:on-click #(re-frame/dispatch [::events/set-employee-iban iban])}
        iban]])))

(defn pagination
  [page total-pages]
  [:nav.pagination.is-small.is-rounded
   {:aria-label "pagination", :role "navigation"}
   [:ul.pagination-list
    (when (> page 2)
      [:li [:a.pagination-link {:aria-label "Goto page 1"
                                :on-click   #(re-frame/dispatch [::events/set-all-accounts-page 1])} "1"]])
    (when (> page 3)
      [:li [:span.pagination-ellipsis]])
    (when (> page 1)
      [:li [:a.pagination-link {:aria-label (str "Goto page " (dec page))
                                :on-click   #(re-frame/dispatch [::events/set-all-accounts-page (dec page)])} (dec page)]])
    [:li
     [:a.pagination-link.is-current
      {:aria-current "page", :aria-label (str "Page " page)} page]]
    (when (not= page total-pages)
      [:li [:a.pagination-link {:aria-label (str "Goto page " (inc page))
                                :on-click   #(re-frame/dispatch [::events/set-all-accounts-page (inc page)])} (inc page)]])
    (when (> total-pages (+ page 2))
      [:li [:span.pagination-ellipsis]])
    (when (> total-pages (inc page))
      [:li [:a.pagination-link {:aria-label (str "Goto page " total-pages)
                                :on-click   #(re-frame/dispatch [::events/set-all-accounts-page total-pages])} total-pages]])]])

(defn employee-buttons
  [transactions employee-iban]
  (let [page (:page transactions)
        all-accounts (:accounts transactions)
        total-pages (quot (+ 19 (count all-accounts)) 20)
        page-accounts (take 20 (drop (* (dec page) 20) all-accounts))]
    [:div
     (when (> total-pages 1) [:div (pagination page total-pages) [:br]])
     [:div.content
      (for [transaction page-accounts] (iban-button transaction employee-iban))]
     (when (> total-pages 1) (pagination page total-pages))]))

(defn intro
  [company-iban]
  [:div.content
   [:p "This is a small demo project for showing some of the possibilities when exposing kafka topics as GraphQL endpoint."]
   [:p "All the components, beside the kafka cluster, and schema registry, are written in clojure."]
   [:p (str "At the right you see some of the transactions of account " company-iban " further on the right are filtering options.")]
   [:p "From the navigation you can go to Bank-employee to see the other accounts, Client to open your own account, or Results to view some test results"]])



(defn left-content
  [selected-nav data]
  (case selected-nav
    :home (intro data)
    :bank-employee (apply employee-buttons data)
    :client (client-notification data)
    :results (results/selection data)
    [:div data]))

(defn transaction-box
  [transaction is-last]
  (let [iban (:iban transaction)
        id (:id transaction)
        changed-by (:changed_by transaction)
        from-to (:from_to transaction)
        descr (:descr transaction)
        direction (:direction transaction)
        new-balance (:new_balance transaction)]
    [:div.box
     (let [map {:id    (str "transaction-box-" id)
                :key   id
                :class (if is-last "will-fade-out" "will-fade-in")}]
       (if is-last
         (assoc map :style {:visibility "hidden"})
         map))
     [:div.columns.is-multiline
      (when (or changed-by from-to)
        [:div.column.is-half-tablet.is-one-third-desktop
         [:p
          (when direction (if (= direction "CREDIT")
                            [:span.icon.is-pulled-left {:style {:color "#95c23d"}} [:i.mdi.mdi-24px.mdi-arrow-up]]
                            [:span.icon.is-pulled-left {:style {:color "#b30000"}} [:i.mdi.mdi-24px.mdi-arrow-down]]))
          (when changed-by [:span.is-pulled-right changed-by])]
         (when from-to [:p from-to])
         (when id [:p id])])
      (when descr
        [:div.column.is-half-tablet.is-one-third-desktop [:p descr]])
      (when (or new-balance iban)
        [:div.column.is-half-tablet.is-one-third-desktop
         (when new-balance [:p new-balance])
         (when iban [:p iban])])]]))

(defn show-transactions
  [transactions]
  [:div#transactions
   (for [transaction (:list transactions)] (transaction-box transaction false))
   (if-let [last-transaction (:last transactions)]
     (transaction-box last-transaction true))])

(defn employee-overview
  [iban transactions]
  (if iban
    [:div
     [:div.notification.is-success
      [:button.delete {:on-click #(re-frame/dispatch [::events/set-employee-iban nil])}]
      [:p (str "You currently follow " iban)]]
     (show-transactions transactions)]
    [:p.notification.is-info "Select an account from the first column to follow the transactions."]))

(defn home-overview
  [transactions]
  [:div
   [:div.notification.is-success
    [:p (str "Showing transactions from the company account.")]]
   (show-transactions transactions)])

(defn transfer-money-form
  [login-status transfer-data]
  (let [amount (atom (:amount transfer-data))
        to (atom (:to transfer-data))
        descr (atom (:descr transfer-data))
        uuid (str (random-uuid))]
    [:form#transfer-form
     [:div.field [:div.control.has-icons-left
                  [:input.input
                   {:type        "number"
                    :step        "1"
                    :min         "0.01"
                    :max         "1000000.00"
                    :placeholder "Amount to transfer"
                    :on-change   #(do (reset! amount (-> % .-target .-value))
                                      (re-frame/dispatch [::events/check-valid-transfer-form @amount @to @descr]))}]
                  [:span.icon.is-left [:i.mdi.mdi-24px.mdi-currency-eur]]]]
     [:div.field [:div.control.has-icons-left
                  [:input.input
                   {:type        "text"
                    :placeholder "To"
                    :on-change   #(do (reset! to (-> % .-target .-value))
                                      (re-frame/dispatch [::events/check-valid-transfer-form @amount @to @descr]))}]
                  [:span.icon.is-left [:i.mdi.mdi-24px.mdi-lock]]]]
     [:div.field [:div.control.has-icons-left
                  [:input.input
                   {:type        "text"
                    :placeholder "Description"
                    :on-change   #(do (reset! descr (-> % .-target .-value))
                                      (re-frame/dispatch [::events/check-valid-transfer-form @amount @to @descr]))}]
                  [:span.icon.is-left [:i.mdi.mdi-24px.mdi-note-text]]]]
     [:div.field [:div.control
                  [:a.button.is-info
                   (if (:valid transfer-data)
                     {:on-click #(do
                                   (.reset (.getElementById js/document "transfer-form"))
                                   (re-frame/dispatch [::re-graph/subscribe
                                                       :ss
                                                       (keyword (str "transfer-" uuid))
                                                       "($amount: Int! $uuid: String! $username: String! $to: String! $token: String! $from: String! $descr: String!){
                                                          money_transfer(amount: $amount uuid: $uuid username: $username to: $to token: $token from: $from descr: $descr)
                                                          {reason success uuid}}"
                                                       {:amount   (-> (js/parseFloat @amount)
                                                                      (* 100)
                                                                      int)
                                                        :username (:username login-status)
                                                        :from     (:iban login-status)
                                                        :token    (:token login-status)
                                                        :to       @to
                                                        :descr    @descr
                                                        :uuid     uuid}
                                                       [::events/on-transfer]]))}
                     {:disabled true})
                   "Transfer"]]]
     (if-let [reason (:reason transfer-data)]
       [:div.notification.is-warning reason])
     [:p]]))

(defn client-overview
  [login-status transfer-data transactions]
  (if (:token login-status)
    [:div
     [:div.notification.is-success
      [:p (str "Showing transactions from " (:iban login-status))]]
     (transfer-money-form login-status transfer-data)
     (show-transactions transactions)]
    [:div.content
     [:p "After login transactions will show here."]
     [:p "Both username and password need to have a minimal of 8 characters. Once a username is taken, you need the set password for access"]]))

(defn middle-content
  [selected-nav data]
  (case selected-nav
    :home (home-overview data)
    :bank-employee (apply employee-overview data)
    :client (apply client-overview data)
    :results [results/result-component data]
    [:p.notification.is-info (str "Something went wrong, " selected-nav " is no valid nav")]))

(defn max-items-button
  [max-items m-i]
  (if (= max-items m-i)
    [:a.button.is-success.is-active
     {:id  (str "max-items-" m-i)
      :key m-i}
     [:p m-i]]
    [:a.button.is-info
     {:id       (str "max-items-" m-i)
      :key      m-i
      :on-click #(re-frame/dispatch [::events/set-max-items m-i])}
     [:p m-i]]))

(defn max-items-buttons
  [max-items]
  [:div.content
   [:p "Select the number of transactions to show."]
   [:div.buttons (for [m-i [5 10 25 50 100 250]]
                   (max-items-button max-items m-i))]])

(defn argument-button
  [a-k label show-arguments]
  [:a.button
   {:id       (str "show-argument-" label)
    :key      a-k
    :class    (if (a-k show-arguments) "is-success" "is-info")
    :on-click #(re-frame/dispatch [::events/toggle-show a-k])}
   (if (a-k show-arguments)
     [:span.icon [:i.mdi.mdi-24px.mdi-eye]]
     [:span.icon [:i.mdi.mdi-24px.mdi-eye-off]])
   [:span label]])

(defn show-argument-buttons
  [show-arguments]
  [:div.content
   [:p "Select which information to show."]
   [:div.buttons
    (for [[a-k label] {:show_iban        "own account"
                       :show_new_balance "new balance"
                       :show_direction   "debit/credit"
                       :show_from_to     "other account"
                       :show_changed_by  "amount changed"
                       :show_descr       "description"}]
      (argument-button a-k label show-arguments))]])

(defn login
  [login-status]
  (if
    (and (:username login-status) (:token login-status))
    [:div.content
     [:p (str "Logged in as: " (:username login-status))]
     [:a.button.is-warning.is-rounded
      {:on-click #(re-frame/dispatch [::events/logout])}
      "Logout"]]
    (let [username (atom (:username login-status))
          password (atom (:password login-status))]
      [:form#login-form
       [:div.field [:div.control.has-icons-left
                    [:input.input
                     {:type        "text"
                      :placeholder "Username"
                      :on-change   #(do
                                      (reset! username (-> % .-target .-value))
                                      (re-frame/dispatch [::events/check-valid-login-form @username @password]))}]
                    [:span.icon.is-left [:i.mdi.mdi-24px.mdi-account]]]]
       [:div.field [:div.control.has-icons-left
                    [:input.input
                     {:type        "password"
                      :placeholder "Password"
                      :on-change   #(do
                                      (reset! password (-> % .-target .-value))
                                      (re-frame/dispatch [::events/check-valid-login-form @username @password]))}]
                    [:span.icon.is-left [:i.mdi.mdi-24px.mdi-lock]]]]
       [:div.field [:div.control
                    [:a.button.is-info.is-fullwidth
                     (if
                       (:valid login-status)
                       {:on-click #(do
                                     (.reset (.getElementById js/document "login-form"))
                                     (re-frame/dispatch [::events/get-account @username @password]))}
                       {:disabled true})
                     "Login"]]]
       (if-let [reason (:reason login-status)]
         [:div.notification.is-warning reason])])))
