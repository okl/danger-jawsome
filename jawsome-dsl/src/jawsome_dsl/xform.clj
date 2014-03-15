(ns jawsome-dsl.xform
  "Implementing a mini-language for jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [diesel.core :refer [definterpreter]]
            [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is how you put things in the registry :)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def- sym-tab (atom {}))

(defn defxform [k v]
  (when (contains? @sym-tab k)
    (log/warnf "Attempting to overwrite already defined function %s" k))
  (swap! sym-tab #(assoc % k v)))

(def defvar defxform)

(defn xform-registry []
  @sym-tab)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Define dat interpreter doe
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; We aren't using a more generic version here
;; either because we don't think it can exist
;; or we understand that the contract is we want
;; to generate functions that take a map (or string)
;; and return 0 or more maps (or strings) -
;; but always maps (or strings).
(defn- seqify [map-str-or-seq]
  (if (or (map? map-str-or-seq)
          (string? map-str-or-seq))
    (list map-str-or-seq)
    (seq map-str-or-seq)))

;; (xform (lookup 'fn-id) & args-to-init-fn) => (fn [m] ...)
(definterpreter l1-interp [reg]
  ['xforms  => :xforms]
  ['xform   => :xform]
  ['lookup  => :lookup]
  ['dethunk => :dethunk])


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

;; An xforms block may look like
;;       (xforms & body) OR
;;       (xforms desc & body)
;; ...descriptions are cool.
(defmethod l1-interp :xforms [[_ & exprs] reg]
  "Returns a function of map | map* => map*"
  (let [exprs (if (string? (first exprs)) (rest exprs) exprs)
        inited-xforms (map #(l1-interp % reg) exprs)
        xforms-fn (xforms-applier inited-xforms)]
    (fn [m-or-ms]
      (xforms-fn (seqify m-or-ms)))))


(defmethod l1-interp :xform [expr reg]
  "Returns a function of map => map* "
  (let [[_ lookup-expr & init-args] expr
        init-fn (l1-interp lookup-expr reg)
        init-args (map #(l1-interp % reg) init-args)]
    (if (= init-fn :missing-fn)
      (let [msg (format "Unable to instantiate xform: %s" expr)]
        (log/errorf msg)
        (throw (RuntimeException. msg)))
      (let [inited-fn (apply init-fn init-args)]
        (comp seqify inited-fn)))))

(defmethod l1-interp :lookup [[_ fn-id] reg]
  (let [init-fn (get reg fn-id)]
    (if (nil? init-fn)
      (do
        (log/errorf "Unknown function %s specified. Available: %s"
                    fn-id (keys reg))
        :missing-fn)
      init-fn)))

(defmethod l1-interp :dethunk [[_ thunk-val-expr] reg]
  (let [thunk (l1-interp thunk-val-expr reg)]
    (if (fn? thunk) (thunk) thunk)))
