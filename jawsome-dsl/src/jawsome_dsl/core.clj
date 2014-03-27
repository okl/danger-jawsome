(ns jawsome-dsl.core
  "Implementing a mini-language for Jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.common :refer [def-]])
  (:require [cheshire.core :as cheshire]
            [jsonschema.type-system.extract :refer [extract-type-simplifying]]
            [jsonschema.type-system.simplify :refer [simplify-types]]
            [diesel.core :refer [definterpreter]]
            [jawsome-dsl.xform :refer [defvar
                                       defxform
                                       l1-interp
                                       xform-registry]]
            [jawsome-dsl.separate-phases :refer [separate-phases]]
            [jawsome-dsl.init-registry :as reg]))

(defmacro log-and-throw [error-msg]
  `(do
     (log/error ~error-msg)
     (throw (RuntimeException. ~error-msg))))

(defmacro log-and-return [prefix-string thing]
  `(do
     (let [pretty-thing# (with-out-str (clojure.pprint/pprint ~thing))
           cleaner-thing# (clojure.string/trim pretty-thing#)]
       (log/info (str ~prefix-string "\n" cleaner-thing#))
       ~thing)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; L2 pipeline interpreter!
;;
;; It has an environment (env), which is a map that the defmethods can put
;; stuff in. So far, we only use it to propagate the current xform-ordering.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def default-env {})

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
  ['denorm-phase => :denorm-phase] ;; this is the one-to-many
  ['schema-phase => :schema-phase] ;; this is the folding fxn (many-to-one)
  ['project-phase => :project-phase] ;; this is the one-to-one
  ['delimiter => :delimiter]
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
(reg/init)

(defmethod pipeline-interp :pipeline [[_ & phases] env]
  (let [[denorm schema project] (map #(pipeline-interp % env) phases)]
    {:denorm denorm
     :schema schema
     :project project}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Denorm phase definitions. A read/xform phase has 0 or more blocks.
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

(defmethod pipeline-interp :denorm-phase [[_ & phases] env]
  (log-and-return "l2 forms that came in: " (cons 'pipeline phases))
  (when (> (count phases) 2)
    (log-and-throw (str "Project phase is not yet implemented; need to gather "
                        "schema after the read phase, then project")))
  (let [separated (separate-phases phases)
        interped (map #(pipeline-interp % env) separated)
        concatted (concat (remove nil? interped))
        l1-forms (log-and-return
                  "l1 forms that came out: "
                  (list* 'xforms
                         "Top-level"
                         concatted))
        denorm-fxn (l1-interp l1-forms (xform-registry))]
    denorm-fxn))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema phase
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- make-a-cumulative-schema []
  (atom nil))

(defn- get-cumulative-schema [schema]
  (deref schema))

(defn- mixin-schema! [cumulative-schema new-schema]
  (swap! cumulative-schema #(if (nil? %)
                                new-schema
                                (simplify-types % new-schema))))

(defn- ->clj-map [record]
  (cond (map? record) record
        (string? record) (read-string record)
        :else (throw
               (RuntimeException. (str "unexpected record-type for " record)))))

(defmethod pipeline-interp :schema-phase [[_ & forms] env]
  (fn [denormed-record-seq]
    (let [cumulative-schema (make-a-cumulative-schema)]
      (doseq [record denormed-record-seq]
        (mixin-schema! cumulative-schema
                       (extract-type-simplifying (->clj-map record))))
      (get-cumulative-schema cumulative-schema))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Project phase
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- project-onto-field-order [some-map field-order]
  (map #(get some-map %) field-order))

(defn field-order [schema]
  ;;TODO memoize me
  (let [fields (get schema :properties)]
    (sort fields)))

(defn- map->xsv [some-map cumulative-schema delimiter]
  (let [sorted-fields (field-order cumulative-schema)
        projected (project-onto-field-order some-map sorted-fields)]
    (clojure.string/join delimiter projected)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO turn me into unit tests

;; (def a "{k1 1, k2_arr 100, k2_idx 0}")
;; (def a-map {"k1" 1, "k2_arr" 100, "k2_idx" 0})
;; (def b #jsonschema.type_system.types.Document{:properties #{"k2_idx" "k1" "k2_arr"}, :map {"k1" #jsonschema.type_system.types.Int{:min 1, :max 12}, "k2_arr" #jsonschema.type_system.types.Str{:min 2, :max 3}, "k2_idx" #jsonschema.type_system.types.Int{:min 0, :max 2}}})
;; (def c "|")
;; (map->xsv a-map b c)

(defmethod pipeline-interp :project-phase [[_ & project-cfg] env]
  (let [delimiter (pipeline-interp (first project-cfg) env)]
    (fn [denormed-record cumulative-schema]
      (map->xsv (->clj-map denormed-record)
                cumulative-schema
                delimiter))))

(defmethod pipeline-interp :delimiter [[_ delimiter] env]
  delimiter)

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO turn me into unit tests
(def p (pipeline-interp '(pipeline
                          (denorm-phase (xform-phase (xforms :denorm)))
                          (schema-phase)
                          (project-phase (delimiter "|"))) {}))
(def denorm (:denorm p))
(def schema (:schema p))
(def project (:project p))
(def raw (list {"a" "1", "b" ["2" "34"]} {"foo" "bazzle"} {"foo" 123}))
(def d (list {"a" "1", "b_arr" "2", "b_idx" 0} {"a" "1", "b_arr" "34", "b_idx" 1} {"foo" "bazzle"} {"foo" 123}))
;;(println (schema d))
(def s (schema d)) ;;#jsonschema.type_system.types.Document{:properties #{"b_idx" "b_arr" :foo :a}, :map {:foo #jsonschema.type_system.types.Union{:union-of #{#jsonschema.type_system.types.Str{:min 6, :max 6} #jsonschema.type_system.types.Int{:min 123, :max 123}}}, "b_idx" #jsonschema.type_system.types.Int{:min 0, :max 1}, "b_arr" #jsonschema.type_system.types.Str{:min 1, :max 2}, :a #jsonschema.type_system.types.Str{:min 1, :max 1}}})
(map #(project % s) d)
