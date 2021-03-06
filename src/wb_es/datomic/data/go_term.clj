(ns wb-es.datomic.data.go-term
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Go-term [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:wbid (:go-term/id entity)
     :label (first (:go-term/name entity))
     :other_names (->> (:go-term/synonym entity)
                       (keep (fn [holder]
                               (if (= "exact" (name (:go-term.synonym/scope holder)))
                                 (:go-term.synonym/text holder)))
                             ))
     :description (first (:go-term/definition entity))}))
