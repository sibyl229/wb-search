(ns wb-es.web.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(defn wrap-query-lower-case [handler]
  (fn [request]
    (handler (update-in request [:params :q] #(some-> % clojure.string/lower-case)))))


(defn get-filter [options]
  (->> []
       (cons (when-let [type-value (:type options)]
               {:term {:page_type type-value}}))
       (cons (when-let [species-value (some->> (:species options)
                                               (clojure.string/lower-case))]
               {:term {:species.key species-value}}))
       (cons (when-let [species-value (some->> (:paper_type options)
                                               (clojure.string/lower-case))]
               {:term {:paper_type species-value}}))
       (filter identity)))


(declare autocomplete)

(defn search [es-base-url index q options]
  (if (:autocomplete options)
    (autocomplete es-base-url index q options)
    (let [query (if (and q (not= (clojure.string/trim q) ""))
                  {;:explain true
                   :sort [:_score
                          {:label.raw {:order :asc}}]
                   :query
                   {:bool
                    {:must [{:bool {:filter (get-filter options)}}
                            {:dis_max
                             {:queries [{:term {:wbid q}}
                                        {:match_phrase {:label {:query q}}}
                                        {:match_phrase {:other_names {:query q
                                                                      :boost 0.9}}}
                                        {:match_phrase {:_all {:query q
                                                               :boost 0.1}}}]
                              :tie_breaker 0.3}}
                            ]}}
                   :highlight
                   {:fields {:wbid {}
                             :wbid_as_label {}
                             :label {}
                             :other_names {}
                             :description {}}}
                   }
                  {:query {:bool {:filter (get-filter options)}}})

          response
          (try
            (http/get (format "%s/%s/_search?size=%s&from=%s"
                              es-base-url
                              index
                              (get options :size 10)
                              (get options :from 0))
                      {:content-type "application/json"
                       :body (json/generate-string query)})
            (catch clojure.lang.ExceptionInfo e
              (clojure.pprint/pprint (ex-data e))
              (throw e)))]
      (json/parse-string (:body response) true))))


(defn autocomplete [es-base-url index q options]
  (let [query {:sort [:_score
                      {:label.raw {:order :asc}}]
               :query
               {:bool
                {:must [{:bool {:filter (get-filter options)}}
                        {:bool
                         {:should [{:match {:wbid.autocomplete_keyword q}}
                                   {:bool {:should [{:match {:label.autocomplete_keyword q}}
                                                    {:match {:label.autocomplete q}}]}}]}}]}}}

        response
        (http/get (format "%s/%s/_search?size=%s"
                          es-base-url
                          index
                          (get options :size 10))
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn search-exact [es-base-url index q options]
  (let [query {:query
               {:bool
                {:must [{:bool {:filter (get-filter options)}}
                        {:bool
                         {:should [{:term {:wbid q}}
                                   {:term {:other_unique_ids q}}
                                   {:term {"label.raw" q}}]}}]}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          index)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn random [es-base-url index options]
  (let [date-seed (.format (java.text.SimpleDateFormat. "MM/dd/yyyy") (new java.util.Date))
        query {:query
               {:function_score
                {:filter {:bool {:filter (get-filter options)}}
                 :functions [{:random_score {:seed date-seed}}]
                 :score_mode "sum"}}}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          index)
                  {:content-type "application/json"
                   :body (json/generate-string query)})]
    (json/parse-string (:body response) true)))


(defn count [es-base-url index q options]
  (if (:autocomplete options)
    (autocomplete es-base-url index q options)
    (let [query (if (and q (not= (clojure.string/trim q) ""))
                  {:query
                   {:bool
                    {:must [{:bool {:filter (get-filter options)}}
                            {:dis_max
                             {:queries [{:term {:wbid q}}
                                        {:match_phrase {:label {:query q}}}
                                        {:match_phrase {:other_names {:query q
                                                                      :boost 0.9}}}
                                        {:match_phrase {:_all {:query q
                                                               :boost 0.1}}}]
                              :tie_breaker 0.3}
                             }]}}}
                  {:query {:bool {:filter (get-filter options)}}})

          response
          (http/get (format "%s/%s/_count"
                            es-base-url
                            index)
                    {:content-type "application/json"
                     :body (json/generate-string query)})]
      (json/parse-string (:body response) true))))

(defn facets [es-base-url index q options]
  (let [query (if (and q (not= (clojure.string/trim q) ""))
                {:dis_max
                 {:queries [{:term {:wbid q}}
                            {:match_phrase {:label {:query q}}}
                            {:match_phrase {:other_names {:query q
                                                          :boost 0.9}}}
                            {:match_phrase {:_all {:query q
                                                   :boost 0.1}}}]
                  :tie_breaker 0.3}
                 }
                {:match_all {}})
        categories-config [{:field :page_type
                            :option :type}
                           {:field :paper_type}
                           {:field :species.key
                            :option :species}
                           {:field :biological_process}
                           {:field :cellular_component}]
        request-body {:query query
                      :size 0
                      :aggs (reduce (fn [result category]
                                      (let [option (or (:option category) (:field category))
                                            field (:field category)]
                                        (assoc result option {:filter {:bool {:must (get-filter (dissoc options option))}}
                                                              :aggs {:categories
                                                                     {:terms {:field field}}}})))
                                    {}
                                    categories-config)}

        response
        (http/get (format "%s/%s/_search"
                          es-base-url
                          index)
                  {:content-type "application/json"
                   :body (json/generate-string request-body)})]
    (json/parse-string (:body response) true)))
