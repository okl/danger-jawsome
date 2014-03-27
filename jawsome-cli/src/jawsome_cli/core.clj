(ns jawsome-cli.core
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-dsl.core :refer [pipeline-interp
                                      field-order]])
  (:gen-class))

(defmulti run
  (fn [key fn opts]
    key))

(defmethod run :denorm [_ denorm-fn _]
  (doseq [line (line-seq (java.io.BufferedReader. *in*))]
    ;;The inner doall is because a single record of input produces
    ;; a (lazy) sequence of records of output.
    (doall
     (map prn (denorm-fn line))))) ;; prn (not println) so that it can be read later

(defmethod run :schema [_ schema-fn _]
  ;;need function which, when invoked, gives you the schema
  ;;  mebbe reads from schema file if it was precomputed
  ;;  mebbe deserializes all the denorm output and computes it
  ;;but this is not the place for such a function. This assumes it needs
  ;;to deserialize all the denorm output to compute the schema
  (let [denorm-stream (java.io.BufferedReader. *in*)
        cumulative-schema (schema-fn (line-seq denorm-stream))]
    (prn cumulative-schema))) ;; prn (not println) so that it can be read later

(defmethod run :project [_ project-fn opts]
  (let [[denorm-output-path schema-output-path] opts
        denorm-stream (java.io.BufferedReader. (clojure.java.io/reader denorm-output-path))
        schema (read-string (slurp schema-output-path))]
    (println (field-order schema))
    (doseq [line (line-seq denorm-stream)]
      (println (project-fn line schema)))))

(defmacro def-cli-pipeline [l2-form]
  `(defn -main [& args#]
     (let [[phase# & opts#] args#
           phase-as-keyword# (keyword phase#)
           fxns# (pipeline-interp ~l2-form {})
           phase-as-fxn# (get fxns# phase-as-keyword#)]
       (run phase-as-keyword#
            phase-as-fxn#
            opts#))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO turn me into unit tests

(def my-pipeline
  '(pipeline
    (denorm-phase (read-phase (xforms :read-json))
                  (xform-phase (xforms :reify :denorm)))
    (schema-phase)
    (project-phase (delimiter "|"))))

(def-cli-pipeline my-pipeline)


;; (def raw (list "{\"a\": \"1\", \"b\": [\"2\", \"34\"]}" "{\"foo\": \"bazzle\"}" "{\"foo\": 123}"))
;; (with-in-str (reduce str (interpose "\n" (map str raw)))
;;   (-main "denorm")) ;; write to /tmp/denorm

;; (def d (list {"a" "1", "b_arr" "2", "b_idx" 0} {"a" "1", "b_arr" "34", "b_idx" 1} {"foo" "bazzle"} {"foo" 123}))
;; (with-in-str (reduce str (interpose "\n" (map str d)))
;;   (-main "schema")) ;; write to /tmp/sch

;; (-main "project" "/tmp/denorm" "/tmp/sch")

;;lein uberjar
;;cat /tmp/raw | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar denorm | tee /tmp/denorm | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar schema > /tmp/schema ; java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar project /tmp/denorm /tmp/schema > /tmp/project
