(ns jawsome-cli.stuff
  "Support for core"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; gen.clj

;; (defmacro gen-job-classes
;;   "Creates gen-class forms for Hadoop job classes from the current
;;   namespace. Now you only need to write three functions:

;;   (defn mapper-map [this key value mapper-context] ...)

;;   (defn reducer-reduce [this key values reducer-context] ...)

;;   (defn tool-run [& args] ...)

;;   The first two functions are the standard map/reduce functions in any
;;   Hadoop job.

;;   The third function, tool-run, will be called by the Hadoop framework
;;   to start your job, with the arguments from the command line.  It
;;   should set up the Job object and call JobClient/runJob, then
;;   return zero on success.

;;   You must also call gen-main-method to create the main method.

;;   After compiling your namespace, you can run it as a Hadoop job using
;;   the standard Hadoop command-line tools."
;;   []
;;   (let [the-name (.replace (str (ns-name *ns*)) \- \_)]
;;     `(do
;;        (gen-class
;;         :name ~the-name
;;         :extends "org.apache.hadoop.conf.Configured"
;;         :implements ["org.apache.hadoop.util.Tool"]
;;         :prefix "tool-"
;;         :main true)
;;        (gen-class
;;         :name ~(str the-name "_mapper")
;;         :extends "org.apache.hadoop.mapreduce.Mapper"
;;         :prefix "mapper-"
;;         :main false))))

;; (defn gen-main-method
;;   "Adds a standard main method, named tool-main, to the current
;;   namespace."
;;   []
;;   (let [the-name (.replace (str (ns-name *ns*)) \- \_)]
;;     (intern *ns* 'tool-main
;;             (fn [& args]
;;               (System/exit
;;                (org.apache.hadoop.util.ToolRunner/run
;;                 (new org.apache.hadoop.conf.Configuration)
;;                 (. (Class/forName the-name) newInstance)
;;                 (into-array String args)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; config.clj

;; (defn- ^String as-str [s]
;;   (cond (keyword? s) (name s)
;;         (class? s) (.getName ^Class s)
;;         (fn? s) (throw (Exception. "Cannot use function as value; use a symbol."))
;;         :else (str s)))

;; (defn ^Configuration configuration
;;   "Returns the configuration for the job."
;;   [^Job job] (.getConfiguration job))

;; (defn- commandline-job-conf-param? [key]
;;   (= (first (as-str key)) \X))

;; (defmulti conf
;;   (fn [job key value]
;;     (or (and (commandline-job-conf-param? key) :X)
;;         key)))

;; allow users to specify parameters via the commandline
;; to set in the job's configuration
;; e.g. -Xmy.foo.value myfoovalue
;; would yield
;; (.set (configuration job) "my.foo.value" "myfoovalue")
;; (defmethod conf :X [^Job job key value]
;;   (.set (configuration job) ^String (subs (as-str key) 1) value))

;; (defmethod conf :job [^Job job key value]
;;   (cond
;;    (nil? value) (throw (IllegalArgumentException. (format "Job %s not found" value)))
;;    (string? value) (conf job :job (load/load-name value))
;;    (fn? value) (conf job :job (value))
;;    :else (doseq [[k v] value] (conf job k v))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; defjob.clj

;; (defn- full-name
;;   "Returns the fully-qualified name for a symbol s, either a class or
;;   a var, resolved in the current namespace."
;;   [s]
;;   (if-let [v (resolve s)]
;;     (cond (var? v) (let [m (meta v)]
;;                      (str (ns-name (:ns m)) \/
;;                           (name (:name m))))
;;           (class? v) (.getName ^Class v))
;;     (throw (Exception. (str "Symbol not found: " s)))))

;; (defmacro defjob
;;   "Defines a job function. Options are the same those in
;;   clojure-hadoop.config.

;;   A job function may be given as the -job argument to
;;   clojure_hadoop.job to run a job."
;;   [sym & opts]
;;   (let [args (reduce (fn [m [k v]]
;;                        (assoc m k
;;                               (cond (keyword? v) (name v)
;;                                     (number? v) (str v)
;;                                     (symbol? v) (full-name v)
;;                                     (instance? Boolean v) (str v)
;;                                     :else v)))
;;                      {} (apply hash-map opts))]
;;     `(def ~sym (constantly ~args))))

;; (def def-cli-pipeline defjob)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; l1-interp-with-schema-fold

;; (defprotocol XformAndSchemaProtocol
;;   (xform [_ m])
;;   (schema-so-far [_]))

;; (deftype XformAndSchema [actual-fxn cumulative-schema]
;;   XformAndSchemaProtocol
;;   (xform [_ m]
;;     (actual-fxn m))
;;   (schema-so-far [_]
;;     (get-cumulative-schema cumulative-schema)))

;; (defn l2->xform-and-schema
;;   ([l2]
;;      (l2->xform-and-schema l2 default-env))
;;   ([l2 env]
;;      (let [l1 (pipeline-interp l2 default-env)
;;            cumulative-schema (make-a-cumulative-schema)
;;            actual-fxn (l1-interp-with-schema-fold l1 (xform-registry) cumulative-schema)]
;;        (XformAndSchema. actual-fxn cumulative-schema))))

;; (defn l1-interp-with-schema-fold [l1-forms xform-registry cumulative-schema]
;;   (let [giant-composed-one-to-many-xform (l1-interp l1-forms xform-registry)]
;;     (fn [one-in]
;;       (let [many-out (giant-composed-one-to-many-xform one-in)
;;             many-out-types (map extract-type-simplifying many-out)
;;             for-side-effects (doall
;;                               (map #(mixin-schema! % cumulative-schema)
;;                                    many-out-types))]
;;         many-out))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; xform-across-a-seq

;; (def s1 (l2->xform-and-schema
;;          '(pipeline
;;            (xform-phase
;;             (xforms :reify :denorm)))))
;; (schema-so-far s1)
;; (xform s1 {"k1" "12341234", "k2" ["10000asdfjl", "20000asdf"]})
;; (schema-so-far s1)

;; (def s2 (l2->xform-and-schema
;;          '(pipeline
;;            (xform-phase
;;             (xforms :reify :denorm)))))
;; (schema-so-far s2)
;; (xform s2 {"k1" "1", "k2" ["100", "200"]})
;; (schema-so-far s2)
;; (xform s2 {"k1" "12", "k2" ["10", "200", "400"]})
;; (schema-so-far s2)

;; (defn xform-across-a-seq [l2-forms seq]
;;   (let [x-and-s (l2->xform-and-schema l2-forms)]
;;     (doseq [record seq]
;;       (doall
;;        (map println (xform x-and-s record))))
;;     (schema-so-far x-and-s)))

;; (xform-across-a-seq '(pipeline (xform-phase (xforms :reify :denorm)))
;;                     (list {"k1" "1", "k2" ["100", "200"]}
;;                           {"k1" "12", "k2" ["10", "200", "400"]}))
;; ;;#jsonschema.type_system.types.Document{:properties #{"k2_idx" "k1" "k2_arr"}, :map {"k1" #jsonschema.type_system.types.Int{:min 1, :max 12}, "k2_arr" #jsonschema.type_system.types.Str{:min 2, :max 3}, "k2_idx" #jsonschema.type_system.types.Int{:min 0, :max 2}}}

;; (defn xform-across-an-input-stream [l2-forms input-stream]
;;   (let [seq (line-seq (java.io.BufferedReader. input-stream))]
;;     (xform-across-a-seq l2-forms seq)))
