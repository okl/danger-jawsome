(ns jawsome-core.xform.xforms.static-injection
  "Implements xform pipeline step: Static Value Injection"
  {:author "Alex Bahouth"
   :date "11/07/2013"}
  (:require [roxxi.utils.collections :refer [deep-merge]]))

(defn static-value-merge
  ([json static-value-map]
     (static-value-merge json static-value-map false))
  ([json static-value-map recursive?]
     (let [merge-fn (if recursive? deep-merge merge)]
       (if (map? json)
         (merge-fn json static-value-map)
         json))))

(defn static-value-merge-fn
  "Given a `static-value-map` returns a function that that accepts a map
and returns the map with the corresponding key-value pairs in `static-value-map`
replacing the values in the supplied map.

If the optional `recursive?` parameter is specified, then the
`static-value-map` will be merged recursively, preserving
any internal property paths in both maps."
  ([static-value-map]
     #(static-value-merge % static-value-map))
  ([static-value-map recursive?]
     #(static-value-merge % static-value-map recursive?)))

(defn default-value-merge
  ([json default-value-map]
     (default-value-merge json default-value-map false))
  ([json default-value-map recursive?]
     (let [merge-fn (if recursive? deep-merge merge)]
       (if (map? json)
         (merge-fn default-value-map json)
         json))))

(defn default-value-merge-fn
  "Given a `default-value-map` returns a function that that accepts a map
and returns the map with the corresponding key-value pairs in `default-value-map`
replacing the values in the supplied map _only if they are missing_.

If the optional `recursive?` parameter is specified, then the
`default-value-map` will be merged recursively, preserving
any internal property paths in both maps."
  ([default-value-map]
     #(default-value-merge % default-value-map))
  ([default-value-map recursive?]
     #(default-value-merge % default-value-map recursive?)))
