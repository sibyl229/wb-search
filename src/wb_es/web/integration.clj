(ns wb-es.web.integration)

(defn- pack-search-obj [doc]
  (let [doc-source (:_source doc)]
    {:id (:wbid doc-source)
     :class (clojure.string/replace (:_type doc) "-" "_")
     :label (:label doc-source)
     :taxonomy (:species doc-source)}))

(defn- get-obj [doc]
  (let [search-obj (pack-search-obj doc)
        doc-source (:_source doc)]
    {:name search-obj
     :description (:description doc-source)
     :taxonomy nil}))

(defn wrap-search [handler]
  (fn [request]
    (let [params (:params request)
          page (get params :page 1)
          page-size (get params :page_size 10)
          params-new (assoc params
                            :size page-size
                            :from (->> page
                                       (Integer.)
                                       (dec)
                                       (* page-size)))
          request-new (assoc request :params params-new)
          response (handler request-new)
          body-new {:matches (->> (get-in response [:body :hits :hits])
                                  (map get-obj))
                    :query (:q params)
                    :querytime (->> (get-in response [:body :took]))
                    :count (get-in response [:body :hits :total])
                    :page page
                    :page_size page-size}]
      (assoc response :body body-new))))

(defn wrap-autocomplete [handler]
  (fn [request]
    (let [params (:params request)
          page-size (get params :page_size 10)
          params-new (assoc params :size page-size)
          response (handler (assoc request :params params-new))
          body-new {:struct (->> (get-in response [:body :hits :hits])
                                 (map pack-search-obj))
                    :query (:q params)
                    :page 1
                    :page_size page-size}]
      (assoc response :body body-new))))

(defn wrap-search-exact [handler]
  (fn [request]
    (let [params (:params request)
          response (handler (assoc request :params {:q (get params :q (:query params))
                                                    :type (get params :type (:class params))
                                                    :species (:species params)}))
          pack-function pack-search-obj
          content-new (->> (get-in response [:body :hits :hits])
                           (map pack-function)
                           (first))]
      (assoc response :body content-new))))

(defn wrap-count [handler]
  (fn [request]
    (let [response (handler request)
          count (get-in response [:body :count])]
      (assoc response :body {:count count}))))

(defn wrap-random [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc response :body (->> (get-in response [:body :hits :hits])
                                 (first)
                                 (get-obj))))))
