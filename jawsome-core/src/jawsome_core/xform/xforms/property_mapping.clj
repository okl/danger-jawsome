(ns jawsome-core.xform.xforms.property-mapping
  "Implements xform pipeline step: Property Renaming, Remapping and Pruning"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.collections :refer [reassoc-many]]))


;; Note: since this transparently delegates to `reassoc-many`, we will
;; skip unit testing this

(defn make-property-remapper
  "Given a property remapping configuration, remaps the
property paths in a map if the source path is valid,
and removes the value if the property path maps to a nil
location.

Property Remapping Configuraton is as defined in
`reassoc-many` in roxxi/clojure-common-utils"
  [property-remapping]
  (fn remap-properties [some-map]
    (reassoc-many some-map property-remapping)))
