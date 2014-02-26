(ns jawsome-dsl.core
  "Implementing a mini-language for Jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [diesel.core :refer [definterpreter]]
            [jawsome-dsl.xform :refer [defvar
                                       defxform]]
            [jawsome-dsl.separate-phases :refer [separate-phases]]
            [jawsome-dsl.init-registry :as reg]
            [roxxi.utils.print :refer [print-expr]]))

(defmacro log-and-throw [error-msg]
  `(do
     (log/error ~error-msg)
     (throw (RuntimeException. ~error-msg))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; L2 pipeline interpreter!
;;
;; It has an environment (env), which is a map that the defmethods can put
;; stuff in. So far, we only use it to propagate the current xform-ordering.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- add-prop-to-env [env prop val]
  (assoc env prop val))
(defn- get-prop-from-env [env prop]
  (get env prop))

(defn- add-ordering [env ordering]
  (add-prop-to-env env :xform-ordering ordering))
(defn- get-ordering [env]
  (get-prop-from-env env :xform-ordering))

(definterpreter pipeline-interp [env]
  ['pipeline => :pipeline]
  ['read-phase => :read-phase]
  ['xform-phase => :xform-phase]
  ['project-phase => :project-phase]
  ['xforms => :xforms]
  ['custom => :custom]
  ['ref => :ref]
  ['dethunk => :dethunk])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pipeline is the top-level block. A pipeline has 0 or 1 read phases,
;; 1 xform phase, and 0 or 1 project phases.
;;
;; Separate the forms into read/xform/project phases, interp each phase,
;; and emit them in one top-level xforms block. Expects each phase to, itself,
;; be an xforms block.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod pipeline-interp :pipeline [[_ & phases] env]
  (let [separated (separate-phases phases)
        interped (map #(pipeline-interp % env) separated)
        concatted (concat (remove nil? interped))]
    (if (nth separated 2)
      (log-and-throw (str "Project phase is not yet implemented; need to gather "
                          "schema after the read phase, then project"))
      (list* 'xforms
             "Top-level"
             concatted))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Phase definitions. A read/xform phase has 0 or more blocks.
;;
;; A block may have an *inherent order* (xforms block, in which the xforms will
;; be rearranged if necessary to lie in the inherent order), or it may be
;; *ordered by user* (custom block, in which the xforms will appear in the order
;; specified by the user).
;;
;; Interp all the blocks, and emit in them in a single xforms block. Expects
;; each block to, itself, be an xforms block.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod pipeline-interp :read-phase [[_ & xforms] env]
  (let [new-env (add-ordering env reg/read-phase-ordering)]
    (list* 'xforms
           "Read phase"
           (map #(pipeline-interp % new-env) xforms))))

(defmethod pipeline-interp :xform-phase [[_ & xforms] env]
  (let [new-env (add-ordering env reg/xform-phase-ordering)]
    (list* 'xforms
           "Xform phase"
           (map #(pipeline-interp % new-env) xforms))))

(defmethod pipeline-interp :project-phase [[_ & project-cfg] env]
  (log/debug "Project-format cfg is" project-cfg)
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Block definitions. A block has 0 or more xforms.
;;
;; Interp all the xforms, reorder them if necessary, and emit them in a single
;; xforms block.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- kw->sym [kw]
  (symbol (name kw)))

(defn- process-elem [kws-and-args elem]
  (if (keyword? elem)
    (conj kws-and-args
          (vector (kw->sym elem)))
    (conj (vec (drop-last kws-and-args))
          (conj (last kws-and-args) elem))))

(defn- partition-by-keywords [xforms]
  (reduce process-elem [] xforms))

(defn- make-comparator [xform-ordering]
  (fn [[xform1 & _] [xform2 & _]]
    (let [idx1 (.indexOf xform-ordering xform1)
          idx2 (.indexOf xform-ordering xform2)]
      (compare idx1 idx2))))

(defn- get-bad-xforms [partitioned-xforms xform-ordering]
  (let [names (map first partitioned-xforms)
        bad (filter #(neg? (.indexOf xform-ordering %))
                    names)]
    (if (empty? bad)
      nil
      bad)))

;; an xform looks like
;;  ('hoist hoist-cfgs other-args)
;;  ('prune-nils)
(defn- reorder-xforms [partitioned-xforms xform-ordering]
  (when-let [bad-xforms (get-bad-xforms partitioned-xforms xform-ordering)]
    (let [msg (format
               "Unrecognized xforms: %s. Can't tell how they fit in the overall order: %s"
               (seq bad-xforms)
               xform-ordering)]
      (log-and-throw msg)))
  (let [comparator (make-comparator xform-ordering)]
    (sort comparator partitioned-xforms)))

(defn- ->l1 [xform env]
  (let [xform-name-with-lookup (list 'lookup (first xform))
        args (map #(pipeline-interp % env) (rest xform))]
    (list* 'xform
           xform-name-with-lookup
           args)))

(defn- process-xforms [xforms doc-string env & {:keys [xform-ordering]}]
  (let [partitioned (partition-by-keywords xforms)
        maybe-reordered (if xform-ordering
                          (reorder-xforms partitioned xform-ordering)
                          partitioned)
        l1-forms (map #(->l1 % env) maybe-reordered)]
    (list* 'xforms
           doc-string
           l1-forms)))

(defmethod pipeline-interp :xforms [[_ & xforms] env]
  (process-xforms xforms "Xforms block" env :xform-ordering (get-ordering env)))

(defmethod pipeline-interp :custom [[_ & xforms] env]
  (process-xforms xforms "Custom block" env))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Registry stuff.
;;
;; If you've added "foo" to the registry using defvar, you dereference it
;; using (ref foo).
;;
;; If you've added a function "bar" to the registry using defvar, you can
;; invoke it at interpet time using (dethunk (ref bar)).
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod pipeline-interp :ref [[_ & stuff] env]
  ;; Just translate "ref" directly into "lookup"
  (list* 'lookup
         stuff))

(defmethod pipeline-interp :dethunk [[_ & stuff] env]
  (list* 'dethunk
         (map #(pipeline-interp % env) stuff)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn -main [pipeline]
  (doseq [line (line-seq (java.io.BufferedReader. *in*))]
    ;;The inner doall is because a single record of input produces
    ;; a (lazy) sequence of records of output.
    (doall
     (map println (pipeline line)))))
