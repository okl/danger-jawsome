(ns jawsome-dsl.xform
  "Implementing a mini-language for jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [diesel.core :refer [definterpreter]]
            [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [jawsome-core.reader.json.xforms.unicode :refer [unicode-recode]]
            [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]]
            [jawsome-core.reader.json.core :refer [make-json-reader-fn]])
  (:require [jawsome-core.xform.xforms.denormalize :refer [make-denormalize]]
            [jawsome-core.xform.xforms.hoist :refer [make-hoist]]
            [jawsome-core.xform.xforms.property-mapping :refer [make-property-remapper]]
            [jawsome-core.xform.xforms.pruning :refer [prune-nils]]
            [jawsome-core.xform.xforms.reify-values :refer [reify-values]]
            [jawsome-core.xform.xforms.static-injection :refer [static-value-merge-fn
                                                                default-value-merge-fn]]
            [jawsome-core.xform.xforms.synonyms :refer [make-value-synonymizer
                                                        make-path-specific-synonymizer]]
            [jawsome-core.xform.xforms.value-type-filter :refer [make-value-type-filter]]))

(def- sym-tab (atom {}))

(defn defxform [k v]
  (when (contains? @sym-tab k)
    (log/warnf "Attemping to overwrite already defined function %s" k))
  (swap! sym-tab #(assoc % k v)))

(def defvar defxform)

(defn xform-registry []
  @sym-tab)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize dat registry doe
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Read phase, ordered xforms
(defxform 'remove-cruft (constantly remove-cruft))
(defxform 'recode-unicode (constantly unicode-recode))
(defxform 'read-json make-json-reader-fn)

;; Xform phase, ordered xforms
(defxform 'hoist make-hoist)
(defxform 'remap-properties make-property-remapper)
(defxform 'reify (constantly reify-values))
(defxform 'translate make-value-synonymizer)
(defxform 'translate-paths make-path-specific-synonymizer)
(defxform 'type-enforce make-value-type-filter)
(defxform 'denorm make-denormalize)
(defxform 'prune-nils (constantly prune-nils))

;; Xform phase, un-ordered xforms aka library xforms
(defxform 'static-values static-value-merge-fn)
(defxform 'default-values default-value-merge-fn)
;; TODO implement these:
;; - remove aka prune-paths
;; - only
;; - drop-if-particular-kv-occurs (e.g. path='/server-status?auto')
;; - drop-if-had-to-type-enforce
;;it is worth remarking that the 'default ordered xforms'
;; can also be treated as library, of course.



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Define dat interpreter doe
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; We aren't using a more generic version here
;; either because we don't think it can exist
;; or we understand that the contract is we want
;; to generate functions that take a map
;; and return 0 or more maps- but always maps.
(defn- seqify [map-str-or-seq]
  (if (or (map? map-str-or-seq)
          (string? map-str-or-seq))
    (list map-str-or-seq)
    (seq map-str-or-seq)))

;; (xform (lookup 'fn-id) & args-to-init-fn) => (fn [m] ...)
(definterpreter xform-phase-interp [reg]
  ['xforms => :xforms]
  ['xform => :xform]
  ['lookup => :lookup]
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

;; (xforms & body)
;; (xforms desc & body) (descriptions are cool)
(defmethod xform-phase-interp :xforms [[_ & exprs] reg]
  "Returns a function of map | map* => map*"
  (let [exprs (if (string? (first exprs)) (rest exprs) exprs)
        inited-xforms (map #(xform-phase-interp % reg) exprs)
        xforms-fn (xforms-applier inited-xforms)]
    (fn [m-or-ms]
      (xforms-fn (seqify m-or-ms)))))


(defmethod xform-phase-interp :xform [expr reg]
  "Returns a function of map => map* "
  (let [[_ lookup-expr & init-args] expr
        init-fn (xform-phase-interp lookup-expr reg)
        init-args (map #(xform-phase-interp % reg) init-args)]
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

(defmethod xform-phase-interp :dethunk [[_ thunk-val-expr] reg]
  (let [thunk (xform-phase-interp thunk-val-expr reg)]
    (if (fn? thunk) (thunk) thunk)))
