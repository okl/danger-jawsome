(ns jawsome-cli.core
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.common :refer [def-]]
            [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-dsl.core :refer [pipeline-interp
                                      field-order
                                      default-env]])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; # CLI options definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def- denorm-options
  [[nil "--input RAW-JSON-FILEPATH"
    "(opt) path to consume raw JSON from. Defaults to stdin"
    :default nil]
   [nil "--output DENORM-FILEPATH"
    "(opt) path to write denormed records to. Defaults to stdout"
    :default nil]
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
   ["-h" "--help"]])

(def- project-options
 [[nil "--delimiter DELIM"
   "(opt) string to use as field delimiter. Defaults to tab"
   :default "\t"]
  [nil "--record-terminator TERM"
   "(opt) string to use as record terminator. Defaults to newline"
   :default "\n"]
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
  (fn [key fn opts]
    key))

;; ## Denorm
(defn- denorm-core [denorm-fn]
  (doseq [raw-json (line-seq *in*)]
    (doseq [denormed (denorm-fn raw-json)]
      (prn denormed))))

(defmethod run :denorm [_ denorm-fn raw-options]
  (let [parsed (parse-opts raw-options denorm-options)
        options (:options parsed)]
    (exit-if-appropriate! "denorm" parsed)
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
  (let [ordered-fields (field-order schema)]
    (print-str (clojure.string/join delimiter ordered-fields))))

(defn- schema-core [schema-fn field-order-path]
  (let [cumulative-schema (schema-fn (line-seq *in*))]
    (when field-order-path
      (spit field-order-path (format-fields cumulative-schema "\n")))
    (prn cumulative-schema)))

(defmethod run :schema [_ schema-fn raw-options]
  (let [parsed (parse-opts raw-options schema-options)
        options (:options parsed)]
    (exit-if-appropriate! "schema" parsed)
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

(defmethod run :project [_ project-fn raw-options]
  (let [parsed (parse-opts raw-options project-options)]
    (exit-if-appropriate! "project" parsed)
    (let [options (:options parsed)
          {denorm-path :input,
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
;; # Auto-generates the -main function that will dispatch to the run multimethod.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro def-cli-pipeline [l2-form]
  `(defn ~(symbol "-main") [& args#]
     (let [[phase# & opts#] args#
           phase-as-keyword# (keyword phase#)
           fxns# (pipeline-interp ~l2-form default-env)
           phase-as-fxn# (get fxns# phase-as-keyword#)]
       (run phase-as-keyword#
            phase-as-fxn#
            opts#))))
