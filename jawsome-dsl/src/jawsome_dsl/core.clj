(ns jawsome-dsl.core
  "Implementing a mini-language for jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [diesel.core :refer [definterpreter]]
            [jawsome-dsl.utils :refer [seqify
                                       safe-seq-apply]]
            [jawsome-dsl.separate-phases :refer [separate-phases]]
            [jawsome-dsl.xform :refer [defvar
                                       defxform
                                       read-phase-ordering
                                       xform-phase-ordering]]
            [roxxi.utils.print :refer [print-expr]]))

(defn- add-ordering [env ordering]
  (assoc env :xform-ordering ordering))
(defn- get-ordering [env]
  (get env :xform-ordering))

(definterpreter pipeline-interp [env]
  ['pipeline => :pipeline]
  ['read-phase => :read-phase]
  ['xform-phase => :xform-phase]
  ['project-phase => :project-phase]
  ['xforms => :xforms]
  ['custom => :custom]
  ['ref => :ref]
  ['dethunk => :dethunk])

(defmethod pipeline-interp :pipeline [[_ & phases] env]
  (let [[read xform project] (separate-phases phases)
        read-block (pipeline-interp read env)
        xform-block (pipeline-interp xform env)
        project-block (if project
                        (pipeline-interp project env)
                        (list))]
    (if project
      #(log/error (str "Project phase is not yet implemented; need to gather "
                       "schema after the read phase, then project"))
      (list* 'xforms
             "Top-level"
             read-block
             xform-block
             '()))))

(defmethod pipeline-interp :read-phase [[_ & xforms] env]
  (let [new-env (add-ordering env read-phase-ordering)]
        (list* 'xforms
               "Read phase"
               (map #(pipeline-interp % new-env) xforms))))

(defmethod pipeline-interp :xform-phase [[_ & xforms] env]
  (let [new-env (add-ordering env xform-phase-ordering)]
    (list* 'xforms
           "Xform phase"
           (map #(pipeline-interp % new-env) xforms))))

(defmethod pipeline-interp :project-phase [[_ & project-cfg] env]
  (log/debug "Project-format cfg is" project-cfg))


(defn- kw-to-sym [kw]
  (symbol (name kw)))

(defn- build-list [kws-and-args elem]
  (if (keyword? elem)
    (conj kws-and-args (vector (kw-to-sym elem)))
    (conj (vec (drop-last kws-and-args))
          (conj (last kws-and-args) elem))))

(defn- make-comparator [xform-ordering]
  (fn [[xform1 & _] [xform2 & _]]
    (let [idx1 (.indexOf xform-ordering xform1)
          idx2 (.indexOf xform-ordering xform2)]
      (when (neg? idx1)
        (throw
         (RuntimeException.
          (str "Unrecognized xform: " xform1))))
      (when (neg? idx2)
        (throw
         (RuntimeException.
          (str "Unrecognized xform: " xform2))))
      (compare idx1 idx2))))


;; an xform looks like
;; (xforms "Read phase"
;;         (xform (lookup 'remove-cruft) fn-init1 fn-init2)
;;         (xform (lookup 'unicode-recode))
(defn- reorder-xforms [partitioned xform-ordering]
  (let [comparator (make-comparator xform-ordering)]
    (sort comparator partitioned)))

(defn- process-xforms [xforms doc-string env & {:keys [xform-ordering]}]
  (let [partitioned (reduce build-list [] xforms)
        sorted (if xform-ordering
                 (reorder-xforms partitioned xform-ordering)
                 partitioned)
        l1-forms (map (fn [xform]
                        (let [fxn (list 'lookup (first xform))
                              args (map #(pipeline-interp % env) (rest xform))]
                          (list* 'xform fxn args)))
                      sorted)]
    (list* 'xforms
           doc-string
           l1-forms)))

(defmethod pipeline-interp :xforms [[_ & xforms] env]
  (process-xforms xforms "Ordered xforms" env :xform-ordering (get-ordering env)))

(defmethod pipeline-interp :custom [[_ & xforms] env]
  (process-xforms xforms "Custom xforms" env))

(defmethod pipeline-interp :ref [[_ & stuff] env]
  ;; Just translate "ref" directly into "lookup"
  (list* 'lookup stuff))

(defmethod pipeline-interp :dethunk [[_ & stuff] env]
  (list* 'dethunk stuff))




;; "xforms" are special ordered xforms
;; "custom" are regular un-ordered xforms, OR custom functions
(defn shawns-fn [json-map]
  json-map)

(defxform 'shawns-fn
  (fn []
    shawns-fn))

(defvar 'hoist-cfg
  [{:properties ["nested_params" "X-Nested-Params"]
    :type "hoist-once-for-property"}
   {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"]
    :type "hoist-once-for-property"
    :prefix "exp_"
    :suffix "_test"}])

(defvar 'foo
  {"-" nil})

(def default-env {})

(def p1
  (pipeline-interp
   '(pipeline
     (read-phase (custom :my-fn)
                 (xforms :remove-cruft
                         :recode-unicode
                         :read-json))
     (xform-phase (custom :shawns-fn)
                  (xforms :hoist (ref hoist-cfg)
                          :reify
                          :translate (ref foo))
                  (custom :my-other-func)
                  (xforms :denorm)))
   default-env))

(def pipeline
  (pipeline-interp
   '(pipeline
     (read-phase (xforms :remove-cruft
                         :recode-unicode
                         :read-json))
     (xform-phase (xforms
                   :hoist [{:properties ["nested_params" "X-Nested-Params"]
                            :type "hoist-once-for-property"}
                           {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"]
                            :type "hoist-once-for-property"
                            :prefix "exp_"
                            :suffix "_test"}]
                   :remap-properties {"num" "renamed_field!"}
                   :reify
                   :translate {"-" nil}
                   :translate-paths {"no" false,
                                     "yes" true,
                                     "false" false,
                                     "true" true,
                                     0 false,
                                     1 true}
                   {["foo"] {"nine" 9 "nueve" 9}
                    ["bar" "sub_bar"] {"ten" "diez"}}
                   :type-enforce {["bool_prop_1"] :boolean
                                  ["bool_prop_2"] :boolean})
                  (custom
                   :static-values false {"syn_prop" 42}
                   :static-values true {"additional_prop" 4422}
                   :default-values true {"syn_prop" 45
                                         "test_prop" 48}
                   :prune-nils true)
                  (xforms :denormalize true)))
   default-env))

(print-expr (pipeline-interp
 '(pipeline
   (xform-phase
    (xforms :reify :hoist :denorm (ref hoist-cfg))
    (custom :shawns-fn)
    (xforms :translate (ref foo))))
 {}))

(defn -main []
  (doseq [line (line-seq (java.io.BufferedReader. *in*))]
    ;;The inner doall is because a single record of input produces
    ;; a (lazy) sequence of records of output.
    (doall
     (map println (pipeline line)))))


;; ;; L2
;; ;; two registries in an env
;; ;; concierge-style over each xforms block
;;  '(pipeline
;;    (read-phase ...)
;;    (xform-phase
;;     (custom myfunc)
;;     (xforms :reify-values
;;             :global-syn (ref x))
;;     (xforms :denormalize-map
;;             :hoist args)
;;     (custom my-other-func)))

;; ;; L1
;; (xforms "Top-level"
;;         (xforms "Read phase"
;;                 (xform (lookup 'remove-cruft) fn-init1 fn-init2)
;;                 (xform (lookup 'unicode-recode))
;;                 (xform (lookup 'read-json) :key-fn true))
;;         (xforms "Transform Phase"
;;                 (xforms
;;                  (xform (lookup fn-id1) fn-init1 fn-init2)
;;                  (xform (lookup fn-id2)))
;;                 (xforms
;;                  (xform (lookup 'reify-values) fn-init1 fn-init2)
;;                  (xform (lookup 'shawns-fn) fn-init1 fn-init2)
;;                  (xform (lookup 'denormalize-map)))))
