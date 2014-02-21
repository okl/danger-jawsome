(ns jawsome-core.xform.xforms.synonyms
  "Implements xform pipeline step: Value Synonym Mapping"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.collections :refer [walk-update-scalars]])
  (:require [roxxi.utils.common :refer [def-]]))


;; # Global synonymization

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
  (let [synonym-fn (make-negative-safe-lookup-fn value=>synonym)]
    (fn [m]
      (walk-update-scalars m synonym-fn))))

(defn value-synonymizer [m value=>synonym]
  (let [synner (make-value-synonymizer value=>synonym)]
    (synner m)))

;; # Path-specific synonymization

(defn- contains-path? [map path]
  (cond
   (empty? path)
   true
   :else
   (let [top-level-prop (first path)]
     (and (map? map)
          (contains? map top-level-prop)
          (contains-path? (get map top-level-prop)
                          (rest path))))))

(defn- translate-syn [json-map [path syns]]
  (let [old-val (get-in json-map path)
        path-matches? (contains-path? json-map path)
        needs-to-be-translated? (contains? syns old-val)]
    (if (and path-matches? needs-to-be-translated?)
      (assoc-in json-map path (get syns old-val))
      json-map)))

(defn- make-path=>syns [default-syns path=>extra-syns]
  (into {}
        (map
         (fn [[path extra-syns]]
           (vector path (into default-syns extra-syns)))
         path=>extra-syns)))

(def- make-path=>syns-memoed
  (memoize make-path=>syns))



(defn make-path-specific-synonymizer [default-syns path=>extra-syns]
  (let [path=>syns (make-path=>syns-memoed default-syns path=>extra-syns)]
    (fn [m]
      (reduce translate-syn m path=>syns))))

(defn path-specific-synonymizer [m default-syns path=>extra-syns]
    (let [synner (make-path-specific-synonymizer default-syns path=>extra-syns)]
      (synner m)))
