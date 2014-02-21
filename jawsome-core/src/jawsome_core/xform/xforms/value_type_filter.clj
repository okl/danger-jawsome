(ns jawsome-core.xform.xforms.value-type-filter
  "Implements xform pipeline step: Property path value-type filtering
 (i.e. type enforcement)"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.collections :refer [dissoc-in project-map]])
  (:require [roxxi.utils.print :refer [print-expr]]))

(def type=>satisfies-type?
  {:number number?
   :boolean #(or (true? %) (false? %))
   :string string?
   :array #(or (vector? %) (list? %) (set? %))
   :map map?})

(defn- remove-offending-fields
  "It's assumed that path and satisfies-type? are coming from a kv
  in a form like so:
    {[\"top-level-prop-which-is-a-map\" \"nested-field-1\"] :number
     [\"top-level-prop-which-is-a-map\" \"nested-field-2\"] :number}"
  [m [path satisfies-type?]]
  (if (satisfies-type? (get-in m path))
    m
    (dissoc-in m path)))


(defn make-value-type-filter [path=>type]
  (let [path=>satisfies-type? (project-map path=>type
                                           :value-xform type=>satisfies-type?)]
    (fn [m]
      (reduce remove-offending-fields m path=>satisfies-type?))))

(defn value-type-filter [m path=>type]
  (let [value-type-filter-fn (make-value-type-filter path=>type)]
    (value-type-filter-fn m)))
