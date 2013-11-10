(ns jawsome-core.xform.xforms.synonyms
  "Implements xform pipeline step: Value Synonym Mapping"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.collections :refer [walk-update-scalars]]))


(defn- make-negative-safe-lookup-fn
  "In the case where a key-value pair might yield a negative value,
this will yield that negative value, rather than the original value.

This is useful when you have a synonym mapping like:
 {\"-\" nil, \"false\" false}"
  [value=>synonym]
  (fn [current-value]
    (if (contains? value=>synonym current-value)
      (get value=>synonym current-value)
      current-value)))

(defn make-value-synonymizer [value=>synonym]
  (fn [some-map]
    (walk-update-scalars
     some-map (make-negative-safe-lookup-fn value=>synonym))))
