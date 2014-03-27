(ns jawsome-cli.core
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-dsl.core :refer [pipeline-interp
                                      field-order]])
  (:import [java.io BufferedReader]))

;; # The run multimethod

;; Implementing run as a multimethod means that we can *separately* wrap each
;; phase with its own appropriate CLI-specific logic. Yay clojure.

;; Note that :denorm and :schema write their output with "prn",
;; so that :project can read it back into Clojure structures.

(defmulti run
  (fn [key fn opts]
    key))

(defmethod run :denorm [_ denorm-fn _]
  (doseq [line (line-seq (BufferedReader. *in*))]
    ;;The inner doall is because a single record of input yields
    ;; a (lazy) sequence of records of output when denormalized.
    (doall
     (map prn (denorm-fn line)))))

(defmethod run :schema [_ schema-fn _]
  (let [denorm-stream (BufferedReader. *in*)
        cumulative-schema (schema-fn (line-seq denorm-stream))]
    (prn cumulative-schema)))

(defmethod run :project [_ project-fn opts]
  (let [[denorm-output-path schema-output-path] opts
        denorm-stream (BufferedReader. (clojure.java.io/reader denorm-output-path))
        schema (read-string (slurp schema-output-path))]
    (println (field-order schema))
    (doseq [line (line-seq denorm-stream)]
      (println (project-fn line schema))))) ;; TODO should this be prn or println?

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
