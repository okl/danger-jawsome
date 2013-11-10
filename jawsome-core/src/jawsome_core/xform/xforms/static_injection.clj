(ns jawsome-core.xform.xforms.static-injection
  "Implements xform pipeline step: Static Value Injection"
  {:author "Alex Bahouth"
   :date "11/07/2013"}
  (:require [roxxi.utils.collections :refer [deep-merge]]))

(defn static-value-merge-fn
  "Given a `static-value-map` returns a function that that accepts a map
and returns the map with the corresponding key-value pairs in `static-value-map`
replacing the values in the supplied map.

If the optional `recursive?` parameter is specified, then the
`static-value-map` will be merged recursively, perserving
any internal property paths in both maps."
  ([static-value-map]
     (static-value-merge-fn static-value-map false))
  ([static-value-map recurisve?]
     (let [merge-fn (if recurisve? deep-merge merge)]
       (fn static-value-overlay [m]
         (if (map? m) (merge-fn m static-value-map) m)))))

(defn default-value-merge-fn
  "Given a `default-value-map` returns a function that that accepts a map
and returns the map with the corresponding key-value pairs in `default-value-map`
replacing the values in the supplied map _only if they are missing_.

If the optional `recursive?` parameter is specified, then the
`default-value-map` will be merged recursively, perserving
any internal property paths in both maps."
  ([default-value-map]
     (default-value-merge-fn default-value-map false))
  ([default-value-map recurisve?]
     (let [merge-fn (if recurisve? deep-merge merge)]
       (fn [m]
         (if (map? m) (merge-fn default-value-map m) m)))))
