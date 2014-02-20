(ns jawsome-dsl.xform
  "Implementing a mini-language for jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [diesel.core :refer [definterpreter]]
            [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:use jawsome-core.xform.xforms.denormalize
        jawsome-core.xform.xforms.hoist
        jawsome-core.xform.xforms.property-mapping
        jawsome-core.xform.xforms.pruning
        jawsome-core.xform.xforms.reify-values
        jawsome-core.xform.xforms.static-injection
        jawsome-core.xform.xforms.synonyms
        jawsome-core.xform.xforms.value-type-filter))

(def- registry (atom {}))


(defn register-xform! [k v]
  (when (contains? @registry k)
    (log/warnf "Attemping to overwrite already defined function %s" k))
  (swap! registry #(assoc % k v)))

(defn xform-registry []
  @registry)

(register-xform! 'prune-nils (constantly prune-nils))
(register-xform! 'reify (constantly reify-values))
(register-xform! 'remap-properties make-property-remapper)
(register-xform! 'value-type-filter make-value-type-filter)
(register-xform! 'translate make-value-synonymizer)
(register-xform! 'translate-paths make-path-specific-synonymizer)
(register-xform! 'static-values static-value-merge-fn)
(register-xform! 'default-values default-value-merge-fn)

;; We aren't using a more generic version here
;; either because we don't think it can exist
;; or we understand that the contract is we want
;; to generate functions that take a map
;; and return 0 or more maps- but always maps.
(defn- seqify [m-or-s]
  (if (map? m-or-s)
    (list m-or-s)
    (seq m-or-s)))

;; (xform (lookup 'fn-id) & args-to-init-fn) => (fn [m] ...)
(definterpreter xform-phase-interp [reg]
  ['xforms => :xforms]
  ['xform => :xform]
  ['lookup => :lookup])


(defn- xforms-applier
  "Takes a collection of functions s.t. each function takes a map => map*
and returns a function that invokes each supplied function on map*

map* is short hand for a sequence of 0 or more maps
"
  [xforms]
  (fn [map*]
    (loop [results map*
           xforms xforms]
      (if (empty? xforms)
        results
        (recur (mapcat (first xforms) results)
               (rest xforms))))))

(defmethod xform-phase-interp :xforms [[_ & exprs] reg]
  "Returns a function of map | map* => map*"
  (let [inited-xforms (map #(xform-phase-interp % reg) exprs)
        xforms-fn (xforms-applier inited-xforms)]
    (fn [m-or-ms]
      (xforms-fn (seqify m-or-ms)))))


(defmethod xform-phase-interp :xform [expr reg]
  "Returns a function of map => map* "
  (let [[_ lookup-expr & init-args] expr
        init-fn (xform-phase-interp lookup-expr reg)]
    (if (= init-fn :missing-fn)
      (let [msg (format "Unable to instantiate xform: %s" expr)]
        (log/errorf msg)
        (throw (RuntimeException. msg)))
      (let [inited-fn (apply init-fn init-args)]
        (comp seqify inited-fn)))))

(defmethod xform-phase-interp :lookup [[_ fn-id] reg]
  (let [init-fn (get reg fn-id)]
    (if (nil? init-fn)
      (do
        (log/errorf "Unknown function %s specified. Available: %s"
                    fn-id (keys reg))
        :missing-fn)
      init-fn)))

(def the-program '(xforms
                    (xforms
                     (xform (lookup translate) {"yes" true "no" false})
                     (xform (lookup reify)))
                    (xforms
                     (xform (lookup remap-properties) {:d "renamed!"})
                     (xform (lookup remap-properties) {:e "Also renamed!"})
                     (xform (lookup reify)))))

(def b (xform-phase-interp the-program (xform-registry)))

(b {:a "yes" :b "no" :c "14" :d "rename_me" :e "ooh me too"})
