(ns jawsome-cli.core
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-dsl.core :refer [pipeline-interp
                                      field-order]])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.java.io :as io]))

;; # CLI options definitions

(def denorm-options
  [[nil "--input RAW-JSON-FILEPATH" "(opt) path to consume raw JSON from. Defaults to stdin"
    :default *in*]
   [nil "--output DENORM-FILEPATH" "(opt) path to write denormed records to. Defaults to stdout"
    :default *out*]
   ["-h" "--help"]])

(def schema-options
  [[nil "--input DENORM-FILEPATH" "(opt) path to consume denormed records from. Defaults to stdin"
    :default *in*]
   [nil "--output SCHEMA-FILEPATH" "(opt) path to write the cumulative schema to. Defaults to stdout"
    :default *out*]
   ["-h" "--help"]])

(def project-options
 [[nil "--delimiter DELIM" "(opt) string to use as field delimiter"
   :default "|"]
  ;;TODO add record delimiter (as opposed to field delimiter)?
  [nil "--input DENORM-FILEPATH" "path to consume denormed records from"]
  [nil "--schema-path SCHEMA-FILEPATH" "path to schema describing the denormed-record-stream"]
  [nil "--header-path HEADER-FILEPATH" "(opt) path to write the xsv header to. Defaults to the value of --output."
   :default nil]
  [nil "--output OUTPUT-FILEPATH" "(opt) path to write xsv-projected records to. Defaults to stdout"
   :default *out*]
  ["-h" "--help"]])

(def top-level-options
  [])

;; # Helper functions

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

;; # The run multimethod

;; Implementing run as a multimethod means that we can *separately* wrap each
;; phase with its own appropriate CLI-specific logic. Yay Clojure!

;; Note that :denorm and :schema write their output with "prn",
;; so that :project can read it back into Clojure structures.

(defmulti run
  (fn [key fn opts]
    key))

(defmethod run :denorm [_ denorm-fn raw-options]
  (let [parsed (parse-opts raw-options denorm-options)
        options (:options parsed)]
    (exit-if-appropriate! "denorm" parsed)
    (with-open [input-stream (io/reader (:input options))
                output-stream (io/writer (:output options))]
      (doseq [raw-json (line-seq input-stream)]
        (doseq [denormed (denorm-fn raw-json)]
          (.write output-stream (prn-str denormed)))))))

(defmethod run :schema [_ schema-fn raw-options]
  (let [parsed (parse-opts raw-options schema-options)
        options (:options parsed)]
    (exit-if-appropriate! "schema" parsed)
    (with-open [denorm-stream (io/reader (:input options))
                output-stream (io/writer (:output options))]
      (let [cumulative-schema (schema-fn (line-seq denorm-stream))]
        (.write output-stream (prn-str cumulative-schema))))))

(defn format-fields [schema delimiter]
  (let [ordered-fields (field-order schema)]
    (println-str (clojure.string/join delimiter ordered-fields))))

(defmethod run :project [_ project-fn raw-options]
  (let [parsed (parse-opts raw-options project-options)]
    (exit-if-appropriate! "project" parsed)
    (let [options (:options parsed)
          {denorm-path :input,
           schema-path :schema-path,
           output-path :output,
           header-path :header-path,
           delimiter :delimiter} options

          schema (read-string (slurp schema-path))
          header (format-fields schema delimiter)]
      (with-open [denorm-stream (io/reader denorm-path)
                  xsv-stream (io/writer output-path)]
        (if header-path
          (spit header-path header)
          (.write xsv-stream header)) ;; defaults to the value of --output
        (doseq [line (line-seq denorm-stream)]
          (.write xsv-stream (println-str (project-fn line schema delimiter))))))))

;; # Auto-generates the -main function that will dispatch to the run multimethod.

(defmacro def-cli-pipeline [l2-form]
  `(defn ~(symbol "-main") [& args#]
     (let [[phase# & opts#] args#
           phase-as-keyword# (keyword phase#)
           fxns# (pipeline-interp ~l2-form {})
           phase-as-fxn# (get fxns# phase-as-keyword#)]
       (run phase-as-keyword#
            phase-as-fxn#
            opts#))))
