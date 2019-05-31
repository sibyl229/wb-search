(ns wb-es.web.index
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response not-found]]
            [wb-es.env :refer [es-base-url release-id]]
            [wb-es.web.core :as web-core]
            [wb-es.web.integration :as web-integration]))

;; hack to let test server to bind to different index
(def ^:private ^:dynamic *index-id*)

(def search-route
  (GET "/search" [q & options]
       (response (web-core/search es-base-url *index-id* q options))))

(def autocomplete-route
  (GET "/autocomplete" [q & options]
       (response (web-core/autocomplete es-base-url *index-id* q options))))

(def search-exact-route
  (GET "/search-exact" [q & options]
       (response (web-core/search-exact es-base-url *index-id* q options))))

(def count-route
  (GET "/count" [q & options]
       (response (web-core/count es-base-url *index-id* q options))))

(def facet-route
  (GET "/facets" [q & options]
       (response (web-core/facets es-base-url *index-id* q options))))


(def random-route
  (GET "/random" [q & options]
       (response (web-core/random es-base-url *index-id* options))))

(def api-lite-routes
  (->
    (context "/" []
      search-route
      autocomplete-route
      search-exact-route
      count-route
      facet-route
      random-route)
    (wrap-routes web-core/wrap-query-lower-case)))

(defroutes integration-routes
  (-> search-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-search))
  (-> autocomplete-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-autocomplete))
  (-> search-exact-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-search-exact))
  (-> count-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-count))
  (-> random-route
      (wrap-routes web-core/wrap-query-lower-case)
      (wrap-routes web-integration/wrap-params)
      (wrap-routes web-integration/wrap-random)))

(defroutes app
  (GET "/" []
       (response {:message "Welcome to the search API."}))
  api-lite-routes
  (context "/integration" []
           integration-routes
           (context "/lite" []
                    (-> api-lite-routes
                        (wrap-routes web-integration/wrap-params)))
           (route/not-found (response {:message "endpoint not found"})))
  (route/not-found (response {:message "endpoint not found"})))


(defn make-handler [index-id]
  (fn handler [request]
    (binding [*index-id* index-id]
      (let [enhanced-handler
            (-> app
                (wrap-cors :access-control-allow-origin [#"https?://(.*\.)?wormbase.org(:\d+)?"]
                           :access-control-allow-methods [:get :put :post :delete])
                (wrap-resource "public")
                (wrap-json-response {:pretty true})
                (wrap-defaults api-defaults))]
        (enhanced-handler request)))
    )
  )

(def handler (make-handler release-id))
