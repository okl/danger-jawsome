(ns jawsome-cli.core
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.common :refer [def-]]
            [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-dsl.core :refer [pipeline-interp
                                      interp-namespaced-pipeline
                                      fields
                                      field-order
                                      default-env]]
            [jawsome-dsl.xform :refer [defvar]])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; # CLI options definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def- pass-thru-option
  ["-X" "--X PASSTHROUGH-CUSTOM-ARG"
   "(opt) If you want to pass through some custom args, simply
use the -X option. You can use as many -Xs as you need; the args will be
passed through as a vector, in order from left to right."
   :default {}
   :assoc-fn (fn [m X kv]
               (let [splits (clojure.string/split kv #":" 2)
                     k (first splits)
                     v (second splits)]
                 (update-in m [X] #(assoc % k v))))])

(def- denorm-options
  [[nil "--input RAW-JSON-FILEPATH"
    "(opt) path to consume raw JSON from. Defaults to stdin"
    :default nil]
   [nil "--output DENORM-FILEPATH"
    "(opt) path to write denormed records to. Defaults to stdout"
    :default nil]
   pass-thru-option
   ["-h" "--help"]])

(def- schema-options
  [[nil "--input DENORM-FILEPATH"
    "(opt) path to consume denormed records from. Defaults to stdin"
    :default nil]
   [nil "--output SCHEMA-FILEPATH"
    "(opt) path to write the cumulative schema to. Defaults to stdout"
    :default nil]
   [nil "--field-order FIELD-ORDER-FILEPATH"
    "(opt) path to write the field-order to. If unspecified, the field-order
will not be written anywhere at all. Column names will be delimited by newlines.
This option is provided in case you want to know what the project-phase header
will look like before you actually get to the project phase."
    :default nil]
   pass-thru-option
   ["-h" "--help"]])

(def- project-options
 [[nil "--delimiter DELIM"
   "(opt) string to use as field delimiter. Defaults to tab"
   :default "\t"
   :default-desc "tab"]
  [nil "--record-terminator TERM"
   "(opt) string to use as record terminator. Defaults to newline"
   :default "\n"
   :default-desc "newline"]
  [nil "--input DENORM-FILEPATH"
   "(opt) path to consume denormed records from. Defaults to stdin"
   :default nil]
  [nil "--schema SCHEMA-FILEPATH"
   "path to schema describing the denormed-record-stream"]
  [nil "--header HEADER-FILEPATH"
   "(opt) path to write the xsv header to. Defaults to the value of --output.
Column names will be delimited by --delimiter"
   :default nil]
  [nil "--output OUTPUT-FILEPATH"
   "(opt) path to write xsv-projected records to. Defaults to stdout"
   :default nil]
  pass-thru-option
  ["-h" "--help"]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; # Helper functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ## Hygienic CLI

(defn- usage [phase-name options-summary]
  (->> [(str "Usage: java -jar your-pipeline-x.y.z-standalone.jar " phase-name " [options]")
        ""
        "Options:"
        options-summary]
       (clojure.string/join \newline)))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn- exit-if-appropriate! [phase-name parsed-options]
  (let [{:keys [options arguments errors summary]} parsed-options]
    (cond
     (:help options)
     (exit 0 (usage phase-name summary))
     (not= (count arguments) 0)
     (exit 1 (usage phase-name summary))
     errors
     (exit 1 (usage phase-name summary)))))

;; ## IO: let's read from/write to a file, but default to *in*/*out*

(defn- using-std-in? [options]
  (nil? (:input options)))
(defn- using-std-out? [options]
  (nil? (:output options)))

(defn- roll-me-a-reader [options]
  (if (using-std-in? options)
    (java.io.BufferedReader. *in*)
    (io/reader (:input options))))
(defn- roll-me-a-writer [options]
  (if (using-std-out? options)
    (java.io.BufferedWriter. *out*)
    (io/writer (:output options))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; # The run multimethod
;;
;; Implementing run as a multimethod means that we can *separately* wrap each
;; phase with its own appropriate CLI-specific logic. Yay Clojure!
;;
;; Note that :denorm and :schema write their output with "prn",
;; so that :project can read it back into Clojure structures.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti run
  (fn [key fn opts cli-time-thunks]
    key))

;; ## Denorm
(defn- denorm-core [denorm-fn]
  (doseq [raw-json (line-seq *in*)]
    (doseq [denormed (denorm-fn raw-json)]
      (prn denormed))))

(defn- invoke-cli-time-thunks! [cli-time-thunks options]
  (let [xs (:X options)]
    (doseq [thunk cli-time-thunks]
      (do
        (log/debug (str "Executing thunk " thunk))
        (thunk xs)))))

(defmethod run :denorm [_ denorm-fn raw-options cli-time-thunks]
  (let [parsed (parse-opts raw-options denorm-options)
        options (:options parsed)]
    (exit-if-appropriate! "denorm" parsed)
    (invoke-cli-time-thunks! cli-time-thunks options)
    (let [i (roll-me-a-reader options)
          o (roll-me-a-writer options)]
      (binding [*in* i
                *out* o]
        (denorm-core denorm-fn))
      (when-not (using-std-in? options)
        (.close i))
      (when-not (using-std-out? options)
        (.close o)))))

;; ## Schema

(defn- format-fields [schema delimiter]
  (let [ordered-fields (field-order (fields schema))]
    (print-str (clojure.string/join delimiter ordered-fields))))

(defn- schema-core [schema-fn field-order-path]
  (let [cumulative-schema (schema-fn (line-seq *in*))]
    (when field-order-path
      (spit field-order-path (format-fields cumulative-schema "\n")))
    (prn cumulative-schema)))

(defmethod run :schema [_ schema-fn raw-options cli-time-thunks]
  (let [parsed (parse-opts raw-options schema-options)
        options (:options parsed)]
    (exit-if-appropriate! "schema" parsed)
    (invoke-cli-time-thunks! cli-time-thunks options)
    (let [i (roll-me-a-reader options)
          o (roll-me-a-writer options)
          field-order-path (:field-order options)]
      (binding [*in* i
                *out* o]
        (schema-core schema-fn field-order-path))
      (when-not (using-std-in? options)
        (.close i))
      (when-not (using-std-out? options)
        (.close o)))))

;; ## Project

(defn- project-core [project-fn header-path header schema delimiter record-terminator]
  (if header-path
    (spit header-path header)
    (do (print header)
        (print record-terminator)))
  (doseq [line (line-seq *in*)]
    (print (project-fn line schema delimiter))
    (print record-terminator))
  (flush)) ;; print doesn't call `flush`... only println does!

(defmethod run :project [_ project-fn raw-options cli-time-thunks]
  (let [parsed (parse-opts raw-options project-options)
        options (:options parsed)]
    (exit-if-appropriate! "project" parsed)
    (invoke-cli-time-thunks! cli-time-thunks options)
    (let [{denorm-path :input,
           schema-path :schema,
           output-path :output,
           header-path :header,
           delimiter :delimiter,
           record-terminator :record-terminator} options

          schema (read-string (slurp schema-path))
          header (format-fields schema delimiter)

          i (roll-me-a-reader options)
          o (roll-me-a-writer options)]
      (binding [*in* i
                *out* o]
        (project-core project-fn
                      header-path
                      header
                      schema
                      delimiter
                      record-terminator))
      (when-not (using-std-in? options)
        (.close i))
      (when-not (using-std-out? options)
        (.close o)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; # Main helpers
;;
;; Note that def-[multi-]cli-pipeline will auto-generate a -main function
;; and do a :gen-class FOR YOU! Magic!
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro mimic-gen-class-directive
  "Functionally equivalent to putting `(:gen-class)` at the ns macro at the top
of your file."
  []
  `(gen-class :name (ns-name *ns*)
              :main true
              :impl-ns *ns*
              :init-impl-ns true))

(def pipeline-name=>pipeline (atom {}))
(def pipeline-name=>cli-time-thunks (atom {}))

(defn main-helper [args]
  (let [[pipeline-name phase & opts] args

        l2-form (get @pipeline-name=>pipeline pipeline-name)
        fxns (interp-namespaced-pipeline l2-form default-env)

        cli-time-thunks (get @pipeline-name=>cli-time-thunks pipeline-name)

        phase-as-keyword (keyword phase)
        phase-as-fxn (get fxns phase-as-keyword)]
    (run phase-as-keyword
         phase-as-fxn
         opts
         (vec cli-time-thunks))))

(defn- extract-cli-time-thunks [& {:keys [cli-time-thunks]
                                   :or {cli-time-thunks nil}}]
  cli-time-thunks)


;; ## def-multi-cli-pipeline

;; In a def-multi-cli-pipeline, every cli-pipeline-def begins
;; with the PIPELINE-NAME, then the forms that comprise the PIPELINE-DEF,
;; then the CLI-TIME-THUNKS.
(defmacro def-multi-cli-pipeline [& cli-pipeline-defs]
  (let [pipelines-map (zipmap (map first cli-pipeline-defs)
                              (map second cli-pipeline-defs))
        thunks-map (zipmap (map first cli-pipeline-defs)
                           (map #(apply extract-cli-time-thunks (rest (rest %))) cli-pipeline-defs))]
    `(do
       (mimic-gen-class-directive)
       (swap! pipeline-name=>pipeline (constantly ~pipelines-map))
       (swap! pipeline-name=>cli-time-thunks (constantly ~thunks-map))
       (defn ~(symbol "-main") [& args#]
         (main-helper args#)))))

;; ## def-cli-pipeline

(def- default-pipeline-name "main")

(defmacro def-cli-pipeline [l2-form & {:keys [cli-time-thunks]
                                       :or {cli-time-thunks nil}}]
  (let [pipelines-map {default-pipeline-name l2-form}
        thunks-map    {default-pipeline-name cli-time-thunks}]
    `(do
       (mimic-gen-class-directive)
       (swap! pipeline-name=>pipeline (constantly ~pipelines-map))
       (swap! pipeline-name=>cli-time-thunks (constantly ~thunks-map))
       (defn ~(symbol "-main") [& args#]
         (main-helper (list* ~default-pipeline-name args#))))))
