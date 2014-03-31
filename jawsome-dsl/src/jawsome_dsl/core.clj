(ns jawsome-dsl.core
  "Implementing a mini-language for Jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as cheshire]
            [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [jsonschema.type-system.extract :refer [extract-type-simplifying]]
            [jsonschema.type-system.simplify :refer [simplify-types]]
            [diesel.core :refer [definterpreter]])
  (:require [jawsome-dsl.denorm :as denorm]
            [jawsome-dsl.separate-phases :refer [separate-top-level-phases]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Top-level pipeline interpreter!
;;
;; It has an environment (env), which is a map that the defmethods can put
;; stuff in. So far, it's only used in the denorm-phase.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def default-env denorm/default-env)

(definterpreter pipeline-interp [env]
  ['pipeline => :pipeline]
  ['denorm-phase => :denorm-phase] ;; this is the one-to-many
  ['schema-phase => :schema-phase] ;; this is the folding fxn (many-to-one)
  ['project-phase => :project-phase] ;; this is the one-to-one
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pipeline is the top-level block. A pipeline may have a denorm phase,
;; a schema phase, and a project phase
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod pipeline-interp :pipeline [[_ & phases] env]
  (let [separated (separate-top-level-phases phases)
        [denorm schema project] (map #(pipeline-interp % env) separated)]
    {:denorm denorm
     :schema schema
     :project project}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Denorm phase (delegates to denorm.clj)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod pipeline-interp :denorm-phase [denorm-phase env]
  (denorm/denorm-phase-sexprs->fn denorm-phase env))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema phase
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- make-a-cumulative-schema []
  (atom nil))

(defn- get-cumulative-schema [schema]
  (deref schema))

(defn- mixin-schema! [cumulative-schema new-schema]
  (swap! cumulative-schema #(if (nil? %)
                                new-schema
                                (simplify-types % new-schema))))

(defn- ->clj-map [record]
  (cond (map? record) record
        (string? record) (read-string record)
        :else (throw
               (RuntimeException. (str "unexpected record-type for " record)))))

(defmethod pipeline-interp :schema-phase [[_ & forms] env]
  (fn [denormed-record-seq]
    (let [cumulative-schema (make-a-cumulative-schema)]
      (doseq [record denormed-record-seq]
        (mixin-schema! cumulative-schema
                       (extract-type-simplifying (->clj-map record))))
      (get-cumulative-schema cumulative-schema))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Project phase
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn field-order [schema]
  (let [fields (get schema :properties)]
    (sort fields)))

(def- field-order-memoized
  (memoize field-order))

(defn- project-onto-field-order [some-map field-order]
  (map #(get some-map %) field-order))

(defn- map->xsv [some-map cumulative-schema delimiter]
  (let [sorted-fields (field-order-memoized cumulative-schema)
        projected (project-onto-field-order some-map sorted-fields)]
    (clojure.string/join delimiter projected)))

(defmethod pipeline-interp :project-phase [[_ & forms] env]
  (fn [denormed-record cumulative-schema delimiter]
    (map->xsv (->clj-map denormed-record)
              cumulative-schema
              delimiter)))
