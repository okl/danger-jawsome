(ns jawsome-dsl.utils
  "Utils that are useful to the rest of jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/02/18"}
  (:require [roxxi.utils.print :refer [print-expr]]))

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
