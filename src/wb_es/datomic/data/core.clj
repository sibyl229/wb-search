(ns wb-es.datomic.data.core
  (:require [wb-es.datomic.data.analysis :as analysis]
            [wb-es.datomic.data.anatomy-term :as anatomy-term]
            [wb-es.datomic.data.antibody :as antibody]
            [wb-es.datomic.data.cds :as cds]
            [wb-es.datomic.data.clone :as clone]
            [wb-es.datomic.data.construct :as construct]
            [wb-es.datomic.data.do-term :as do-term]
            [wb-es.datomic.data.expression-cluster :as expression-cluster]
            [wb-es.datomic.data.expr-pattern :as expr-pattern]
            [wb-es.datomic.data.expr-profile :as expr-profile]
            [wb-es.datomic.data.feature :as feature]
            [wb-es.datomic.data.gene :as gene]
            [wb-es.datomic.data.gene-class :as gene-class]
            [wb-es.datomic.data.go-term :as go-term]
            [wb-es.datomic.data.interaction :as interaction]
            [wb-es.datomic.data.molecule :as molecule]
            [wb-es.datomic.data.paper :as paper]
            [wb-es.datomic.data.phenotype :as phenotype]
            [wb-es.datomic.data.strain :as strain]
            [wb-es.datomic.data.variation :as variation]
            [wb-es.datomic.data.util :as data-util]))

(defn create-document
  "returns document of the desirable type"
  [entity]
  (let [constructor-function
        (case (data-util/get-ident-attr entity)
          :analysis/id analysis/->Analysis
          :anatomy-term/id anatomy-term/->Anatomy-term
          :antibody/id antibody/->Antibody
          :cds/id cds/->Cds
          :clone/id clone/->Clone
          :construct/id construct/->Construct
          :do-term/id do-term/->Do-term
          :expression-cluster/id expression-cluster/->Expression-cluster
          :expr-pattern/id expr-pattern/->Expr-pattern
          :expr-profile/id expr-profile/->Expr-profile
          :feature/id feature/->Feature
          :gene/id gene/->Gene
          :gene-class/id gene-class/->Gene-class
          :go-term/id go-term/->Go-term
          :interaction/id interaction/->Interaction
          :molecule/id molecule/->Molecule
          :paper/id paper/->Paper
          :phenotype/id phenotype/->Phenotype
          :strain/id strain/->Strain
          :variation/id variation/->Variation
          (throw (Exception. "Not sure how to handle the data type. Throw an error to let you know")))]
    (constructor-function entity)))
