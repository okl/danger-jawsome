(ns jawsome-core.xform.xforms.log
  "Implements xform pipeline step: log"
  {:author "Matt Halverson"
   :date "2014/04/15"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [clojure.tools.logging :as logger]))

(defn log
  ([m]
     (log :info m))
  ([level m]
     (logger/log (keyword level) m)
     m))

(defn make-log
  ([]
     (make-log :info))
  ([level]
     (fn [m]
       (log (keyword level) m))))
