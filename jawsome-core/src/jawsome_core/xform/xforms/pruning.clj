(ns jawsome-core.xform.xforms.pruning
  "Implements xform pipeline step: Null Pruning"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.collections :refer [prune-map-scalars]]))

(defn prune-nils
  "Removes all property paths that ultimately terminate at a nil"
  [some-map]
  (prune-map-scalars some-map nil?))
