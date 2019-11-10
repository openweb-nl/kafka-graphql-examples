(ns nl.openweb.test.analysis
  (:require [clojure.data.json :as json]
            [kixi.stats.core :as kixi]
            [kixi.stats.distribution :refer [quantile]]))

(defn valid
  [row nth-in-row]
  (when (< nth-in-row (count row))
    (let [value (nth row nth-in-row)]
      (if
        (coll? value)
        (seq value)
        (not (or (nil? value) (Double/isNaN value)))))))

(defn get-statistic
  [coll nth-in-row statistic]
  (->> coll
       (filter #(valid % nth-in-row))
       (map #(nth % nth-in-row))
       (flatten)
       (transduce identity statistic)))

(defn get-latency-percentile
  [coll percentile] coll
  (let [distribution (->> coll
                          (filter #(valid % 2))
                          (map #(nth % 2))
                          (transduce identity kixi/histogram))]
    (quantile distribution percentile)))

(defn get-generator-latency-percentile
  [coll percentile]
  (let [distribution (->> coll
                          (filter #(valid % 4))
                          (map #(nth % 4))
                          (flatten)
                          (transduce identity kixi/histogram))]
    (quantile distribution percentile)))

(defn vega-item [category [generator-count data-rows]]
  {
   :category                  category
   :generators                generator-count
   :average-latency           (get-statistic data-rows 2 kixi/mean)
   :err-latency               (get-statistic data-rows 2 kixi/standard-error)
   :max-latency               (get-statistic data-rows 2 kixi/max)
   :min-latency               (get-statistic data-rows 2 kixi/min)
   :99-latency                (get-latency-percentile data-rows 0.99)
   :average-generator-latency (get-statistic data-rows 4 kixi/mean)
   :err-generator-latency     (get-statistic data-rows 4 kixi/standard-error)
   :max-generator-latency     (get-statistic data-rows 4 kixi/max)
   :min-generator-latency     (get-statistic data-rows 4 kixi/min)
   :99-generator-latency      (get-generator-latency-percentile data-rows 0.99)
   :transactions              (+ (count data-rows) (get-statistic data-rows 4 kixi/count))
   :transactions-per-second   (+ 1 (/ (get-statistic data-rows 4 kixi/count) (count data-rows)))
   :average-db-ch-cpu         (get-statistic data-rows 5 kixi/mean)
   :err-db-ch-cpu             (get-statistic data-rows 5 kixi/standard-error)
   :average-db-ge-mem         (get-statistic data-rows 6 kixi/mean)
   :err-db-ge-mem             (get-statistic data-rows 6 kixi/standard-error)
   :average-db-cpu            (get-statistic data-rows 7 kixi/mean)
   :err-db-cpu                (get-statistic data-rows 7 kixi/standard-error)
   :average-db-mem            (get-statistic data-rows 8 kixi/mean)
   :err-db-mem                (get-statistic data-rows 8 kixi/standard-error)
   :average-ch-cpu            (get-statistic data-rows 9 kixi/mean)
   :err-ch-cpu                (get-statistic data-rows 9 kixi/standard-error)
   :average-ch-mem            (get-statistic data-rows 10 kixi/mean)
   :err-ch-mem                (get-statistic data-rows 10 kixi/standard-error)
   :average-kb-cpu            (get-statistic data-rows 11 kixi/mean)
   :err-kb-cpu                (get-statistic data-rows 11 kixi/standard-error)
   :average-kb-mem            (get-statistic data-rows 12 kixi/mean)
   :err-kb-mem                (get-statistic data-rows 12 kixi/standard-error)
   :average-ge-cpu            (get-statistic data-rows 13 kixi/mean)
   :err-ge-cpu                (get-statistic data-rows 13 kixi/standard-error)
   :average-ge-mem            (get-statistic data-rows 14 kixi/mean)
   :err-ge-mem                (get-statistic data-rows 14 kixi/standard-error)
   :data-points               (count data-rows)
   })

(defn group-by-load
  [data-rows]
  (group-by (fn [row] (nth row 3)) data-rows))

(defn group
  [data-rows]
  (if (vector? data-rows)
    (group-by-load data-rows)
    (apply merge-with into (map group-by-load data-rows))))

(defn data-rows->vega [category data-rows]
  (map (partial vega-item category) (group data-rows)))

(defn raw->vega
  [data]
  (reduce-kv (fn [i category data-rows] (into i (data-rows->vega category data-rows))) [] data))

(defn line-plot
  ([data-url category-name y-value y-title]
   {:width  800,
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
                         :point {:tooltip {:content "data"}}}
              }]})
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
            :mark     {:type "errorbar"}
            })))

(defn process
  [category data]
  (let [path (str "frontend/resources/public/data/" category ".json")
        vega-items (raw->vega data)]
    (spit path (json/write-str vega-items))))
