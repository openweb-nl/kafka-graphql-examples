(ns nl.openweb.bank.results
  (:require [cljsjs.vega]
            [cljsjs.vega-lite]
            [cljsjs.vega-embed]
            [cljsjs.vega-tooltip]
            [nl.openweb.bank.routes :as routes]
            [reagent.core :as r]))

(def categories {:linger-ms-config "linger.ms config"})

(def x-values {:average-latency           "Average latency (ms)"
               :max-latency               "Max latency (ms)"
               :min-latency               "Min latency (ms)"
               :99-latency                ".99 percentile latency (ms)"
               :average-generator-latency "Average generator latency (ms)"
               :max-generator-latency     "Max generator latency (ms)"
               :min-generator-latency     "Min generator latency (ms)"
               :99-generator-latency      ".99 percentile generator latency (ms)"
               :transactions              "Total amount of transactions (count)"
               :transactions-per-second   "Transactions per second (count/second)"
               :average-db-ch-cpu         "Average cpu handler database (% from total)"
               :average-db-ch-mem         "Average mem handler database (MiB)"
               :average-db-ge-cpu         "Average cpu endpoint database (% from total)"
               :average-db-ge-mem         "Average mem endpoint database (MiB)"
               :average-ch-cpu            "Average cpu command-handler (% from total)"
               :average-ch-mem            "Average mem command-handler (MiB)"
               :average-kb-cpu            "Average cpu kafka broker (% from total)"
               :average-kb-mem            "Average mem kafka broker (MiB)"
               :average-ge-cpu            "Average cpu graphql endpoint (% from total)"
               :average-ge-mem            "Average mem graphql endpoint (MiB)"
               :data-points               "Amount of measurements (count)"})

(defn select-button
  [category x-value is-change-category]
  [:a.button
   {:class "is-success"
    :href  (routes/url-for :results :category category :x-value x-value)
    :key   (str category "-" x-value)}
   (if is-change-category category x-value)])

(defn selection
  [results]
  (let [{:keys [category x-value]} results]
    [:div.content
     [:p "Selected category:\u00A0" [:strong (category categories)]]
     [:p "Selected x-value:\u00A0" [:strong (x-value x-values)]]
     [:p "Select x-value:"]
     [:div.buttons (for [new-x-value (keys (dissoc x-values x-value))]
                     (select-button (name category) (name new-x-value) false))]]))

(defn line-plot
  ([data-url category-name y-value y-title]
   {:width  500,
    :height 500,
    :config {:legend {:titleFont "Roboto" :labelFont "Roboto"}
             :axis   {:titleFont "Roboto" :labelFont "Roboto"}}
    :data   {:url data-url}
    :layer  [{:encoding {:x     {:field "generators"
                                 :type  "quantitative"
                                 :title "Amount of generators"}
                         :y     {:field y-value
                                 :type  "quantitative"
                                 :title y-title}
                         :color {:field "category"
                                 :type  "nominal"
                                 :title category-name}}
              :mark     {:type  "line"
                         :point {:tooltip {:content "data"}}}}]})
  ([vega-items category-name y-value y-title y-err]
   (update (line-plot vega-items category-name y-value y-title) :layer conj
           {
            :encoding {:x      {:field "generators"
                                :type  "quantitative"
                                :title "Amount of generators"}
                       :y      {:field y-value
                                :type  "quantitative"
                                :title y-title}
                       :yError {:field y-err}
                       :color  {:field "category"
                                :type  "nominal"
                                :title category-name}}
            :mark     {:type "errorbar"}})))

(defn get-spec
  [c-value y-value]
  (let [data-url (str "/data/" (name c-value) ".json")
        c-name (c-value categories)
        y-name (name y-value)
        y-title (y-value x-values)
        y-err (clojure.string/replace y-name #"average" "err")]
    (if (= y-err y-name)
      (line-plot data-url c-name y-name y-title)
      (line-plot data-url c-name y-name y-title y-err))))

(defn render-vega
  ([results elem]
   (let [{:keys [category x-value]} results
         spec (clj->js (get-spec category x-value))
         opts {:renderer "canvas"
               :mode     "vega-lite"}]
     (js/vegaEmbed elem spec (clj->js opts)))))

(defn result-component
  [result]
  (r/create-class
    {:display-name          "vega-lite"
     :component-did-mount   (fn [this] (render-vega result (r/dom-node this)))
     :component-will-update (fn [this [_ new-result]] (render-vega new-result (r/dom-node this)))
     :reagent-render        (fn [_] [:div#vis])}))


