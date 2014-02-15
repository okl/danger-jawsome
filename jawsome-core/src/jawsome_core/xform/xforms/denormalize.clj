(ns jawsome-core.xform.xforms.denormalize
  "Implements xform pipeline step: Denormalize"
  {:author "Matt Halverson"
   :date "2/14/2014"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [denormal.core :refer [denormalize-map]]))

;; It's just that easy.
(def denormalize denormalize-map)
