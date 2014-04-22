(ns jawsome-core.xform.xforms.pruning
  "Implements xform pipeline step: Null Pruning"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [roxxi.utils.collections :refer [prune-map-scalars
                                             dissoc-in
                                             contains-path?
                                             mask-map]]))

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
  [initial-map list-of-paths]
  (let [pruner (make-prune-paths list-of-paths)]
    (pruner initial-map)))

;; # keep-paths

;; (defn make-keep-paths [mask]
(defn- paths->mask [list-of-paths]
  (let [sanitized-list-of-paths (map vectorify list-of-paths)]
    (reduce (fn [m path] (assoc-in m path true))
            {}
            sanitized-list-of-paths)))

(defn make-keep-paths [list-of-paths]
  (let [mask (paths->mask list-of-paths)]
    (fn [initial-map]
      (or (mask-map initial-map mask)
          {}))))

(defn keep-paths [initial-map list-of-paths]
  (let [pruner (make-keep-paths list-of-paths)]
    (pruner initial-map)))
