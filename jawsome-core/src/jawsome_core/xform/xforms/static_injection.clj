(ns jawsome-core.xform.xforms.static-injection
  "Implements xform pipeline step 6: Static Value Injection"
  {:author "Alex Bahouth"
   :date "11/07/2013"}
  (:require roxxi.utils.collections :refer [deep-merge]))

(defn static-value-merge-fn
  "Given a `static-value-map` returns a function that that accepts a map
and returns the map with the corresponding key-value pairs in `static-value-map`
replacing the values in the supplied map.

If the optional `recursive?` parameter is specified, then the
`static-value-map` will be merged recursively, perserving
any internal property paths in both maps."
  ([static-value-map]
     (recur static-value-map false))
  ([static-value-map recurisve?]
     (let [merge-fn (if recurisve? deep-merge merge)]
       (fn static-value-overlay [some-map]
         (if (map? some-map)
           (merge-fn some-map static-value-map)
           some-map)))))

(defn default-value-merge-fn
  "Given a `default-value-map` returns a function that that accepts a map
and returns the map with the corresponding key-value pairs in `default-value-map`
replacing the values in the supplied map _only if they are missing_.

If the optional `recursive?` parameter is specified, then the
`default-value-map` will be merged recursively, perserving
any internal property paths in both maps."
  ([default-value-map]
     (recur default-value-map false))
  ([default-value-map recurisve?]
     (let [merge-fn (if recurisve? deep-merge merge)]
       (fn [some-map]
         (if (map? some-map)
           (merge-fn default-value-map some-map)
           some-map)))))
