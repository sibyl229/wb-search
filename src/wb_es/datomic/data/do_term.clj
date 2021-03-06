;; name
;; definition

(ns wb-es.datomic.data.do-term
  (:require [datomic.api :as d]
            [wb-es.datomic.data.util :as data-util]))

(deftype Do-term [entity]
  data-util/Document
  (metadata [this] (data-util/default-metadata entity))
  (data [this]
    {:page_type "disease"
     :wbid (:do-term/id entity)
     :label (:do-term/name entity)
     :other_names (->> (:do-term/synonym entity)
                       (keep (fn [holder]
                               (if (= "exact" (name (:do-term.synonym/scope holder)))
                                 (:do-term.synonym/text holder)))
                             ))
     :description (:do-term/definition entity)}))
