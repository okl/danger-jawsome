(ns jawsome-core.common-utils
  "Utils that are useful to the rest of jawsome-core"
  {:author "Matt Halverson"
   :date "2014/02/11"}
  (:require [roxxi.utils.print :refer [print-expr]]))

(defmacro defregistry [registry-name xform-list]
  `(def ~registry-name
     (let [sym-fn-pairs# (map #(vector % @(resolve %))
                             ~xform-list)
           registry# (into {} sym-fn-pairs#)]
       registry#)))

(defn seqify [thing]
  (if (or (seq? thing)
          (vector? thing)
          (list? thing))
    thing
    (list thing)))

;; This means the contract for an xform is now
;;     input  = a single record
;;     output = EITHER a single record OR a seq of records
(defn safe-seq-apply [xform input options]
  "Adapts the xform to accept a single record OR a seq of records as input,
and produce a flattened seq of records as output (if it doesn't already)."
  (let [input-seq (seqify input)
        possibly-nested-output-seq (map #(apply xform (conj options %))
                                        input-seq)]
    (flatten possibly-nested-output-seq)))
