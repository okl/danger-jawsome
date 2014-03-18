(ns jawsome-core.xform.xforms.pruning
  "Implements xform pipeline step: Null Pruning"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.collections :refer [prune-map-scalars
                                             dissoc-in
                                             contains-path?]]))

(defn prune-nils
  "Removes all property paths that ultimately terminate at a nil"
  [m]
  (prune-map-scalars m nil?))

(defn make-prune-nils []
  prune-nils)

;; ## Removing keys
(defn- vectorify [thing]
  (if (vector? thing)
    thing
    (vector thing)))

(defn- remove-path [m path]
  (if (contains-path? m path)
    (dissoc-in m path)
    m))

(defn make-prune-paths [list-of-paths]
  (let [sanitized-paths (map vectorify list-of-paths)]
    (fn [initial-map]
      (reduce remove-path initial-map sanitized-paths))))

(defn prune-paths
  "Removes all specified property paths (regardless of their value)"
  [inital-map list-of-paths]
  (let [pruner (make-prune-paths list-of-paths)]
    (pruner inital-map)))
