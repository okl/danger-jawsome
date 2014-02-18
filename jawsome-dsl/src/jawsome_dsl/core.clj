(ns jawsome-dsl.core
  "Implementing a mini-language for jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.tools.logging :as log]
            [diesel.core :refer [definterpreter]]
            [jawsome-core.reader.json.core :as r]
            [jawsome-core.xform.core :as x]
            [jawsome-dsl.separate-phases :refer [separate-phases]]
            [roxxi.utils.print :refer [print-expr]]))

(def j "{\"url_encoded_thing\": \"http://domain.com/T4xGXfIqO3f6g',%20decoded='\\xaaf\\xbd\\x10]\\xc1\\x1d\\xa0\\x111\\xbf\\x9b\\xe0\\xb0\\x0f\\x15')\"}")
(def k "{\"num\": 1, \"num_as_str\": \"2\", \"str_prop\": \"this is a str\", \"bool_prop_1\": true, \"bool_prop_2\": \"this is not a bool\", \"array_prop\": [1, 2, 3], \"syn_prop\": \"-\"}")
(def l {"nested_params" {"hoist1" "foo" "hoist2" "bar"}
        "nested_experiment_params" {"hoist3" "baz"}
        "X-Nested-Params" {"hoist4" "foo" "hoist5" "bar"}
        "X-Nested-Experiment-Params" {"hoist6" "baz"}
        "shouldn't get hoisted" {"hoist7" "quux"}
        "denorm_prop" ["a" "b"]})
(def m {"foo" "nine"
        "bar" {"sub_bar" "ten"}
        "baz" "ten"})

(def ^:dynamic *registry* nil)
(defmacro with-registry [r & body]
  `(binding [*registry* ~r]
     ~@body))

(definterpreter pipeline-interp []
  ['pipeline => :pipeline]
  ['read-phase => :read-phase]
  ['xform-phase => :xform-phase]
  ['project-phase => :project-phase]
  ['xforms => :xforms]
  ['xform => :xform])

(defmethod pipeline-interp :pipeline [[_ & phases]]
  (let [[read xform project] (separate-phases phases)
        read-fn (if read
                  (pipeline-interp read)
                  list)
        xform-fn (pipeline-interp xform)
        project-fn (if project
                     (pipeline-interp project)
                     nil)]
    (if project-fn
      #(log/debug "need to gather schema after the read phase, then project")
      #(map xform-fn (read-fn %)))))

(defmethod pipeline-interp :read-phase [[_ xforms]]
  (with-registry r/xform-registry
    (let [xform (pipeline-interp xforms)
          composed-xform (comp list xform)
          json-reader (r/make-json-reader :pre-xform composed-xform)]
      #(r/read-str json-reader %))))

(defmethod pipeline-interp :xform-phase [[_ xforms]]
  (with-registry x/xform-registry
    (pipeline-interp xforms)))

(defmethod pipeline-interp :project-phase [[_ & project-cfg]]
  (log/debug "Project-format cfg is" project-cfg))

(defmethod pipeline-interp :xforms [[_ & xforms]]
  "Returns a single function, which is the composition of
all the xforms (with their arguments) in the order specified"
  (let [partitioned (partition-by keyword? xforms)
        xform*enabled?*args (map #(apply concat %) (partition 2 partitioned))
        parseable-xforms (map #(list 'xform %) xform*enabled?*args)
        xforms (if (empty? parseable-xforms)
                 [identity]
                 (map pipeline-interp parseable-xforms))]
    (apply comp (reverse xforms))))

(defn- enabled? [xform]
  (true? (second xform)))
(defn- keyword-to-xform [keyword registry]
  (get registry (symbol (name keyword))))
(defmethod pipeline-interp :xform [[_ xform]]
  (if (enabled? xform)
    (let [k (first xform)
          fxn (keyword-to-xform k *registry*)
          args (drop 2 xform)]
      #(apply fxn (conj args %)))
    (do
      (log/debug "xform is disabled, skipping it:" xform)
      identity)))


(def pipeline
  (pipeline-interp
   '(pipeline
     (read-phase (xforms
                  :remove-cruft true
                  :unicode-recode true))
     (xform-phase (xforms
                   :hoist true [{:properties ["nested_params" "X-Nested-Params"]
                                 :type "hoist-once-for-property"}
                                {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"]
                                 :type "hoist-once-for-property"
                                 :prefix "exp_"
                                 :suffix "_test"}]
                   :property-remapper true {"num" "renamed_field!"}
                   :reify-values true
                   :global-synonymizer true {"-" nil}
                   :path-specific-synonymizer true {"no" false,
                                                    "yes" true,
                                                    "false" false,
                                                    "true" true,
                                                    0 false,
                                                    1 true}
                                                   {["foo"] {"nine" 9 "nueve" 9}
                                                    ["bar" "sub_bar"] {"ten" "diez"}}
                   :value-type-filter true {["bool_prop_1"] :boolean
                                            ["bool_prop_2"] :boolean}
                   :static-value-merge false {"syn_prop" 42}
                   :static-value-merge true {"additional_prop" 4422}
                   :default-value-merge true {"syn_prop" 45
                                              "test_prop" 48}
                   :prune-nils true
                   :denormalize true)))))



(defn -main []
  (doseq [line (line-seq (java.io.BufferedReader. *in*))]
    ;;The inner doall is because a single record of input produces
    ;; a (lazy) sequence of records of output.
    (doall
     (map println (pipeline line)))))
