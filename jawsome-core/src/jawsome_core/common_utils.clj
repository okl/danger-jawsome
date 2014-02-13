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
