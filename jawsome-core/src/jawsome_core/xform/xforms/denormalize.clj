(ns jawsome-core.xform.xforms.denormalize
  "Implements xform pipeline step: Denormalize"
  {:author "Matt Halverson"
   :date "2/14/2014"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [roxxi.utils.collections :refer [project-map]])
  (:require [denormal.core :refer [denormalize-map]]))

(defn- namify [maybe-keyword]
  "denormalize-map converts some of the keys into keywords.
This function is useful for converting them back."
  (if (keyword? maybe-keyword)
    (name maybe-keyword)
    maybe-keyword))

(defn denormalize [json-map & optional-args]
  (let [args (conj optional-args json-map)
        denormalized-jsons (apply denormalize-map args)]
    (map #(project-map % :key-xform namify)
         denormalized-jsons)))
