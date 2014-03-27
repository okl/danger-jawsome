(ns jawsome-cli.test-pipeline
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-cli.core :refer [def-cli-pipeline]])
  (:require [jawsome-dsl.xform :refer [defxform]]))

(defn get-time [^String log-line]
  (if (not (.startsWith log-line "["))
    nil
    (let [idx (.indexOf log-line "]")]
      (subs log-line 1 idx))))

(defn strip-up-to-first-curly [^String log-line]
  (let [idx (.indexOf log-line "{")]
    (if (= -1 idx)
      log-line
      (subs log-line idx))))

(defn assoc-time-and-strip-start [log-line]
  (let [timestamp (get-time log-line)
        start-stripped (strip-up-to-first-curly log-line)
        stripped (clojure.string/trim start-stripped)
        len (count stripped)]
    (if (not (.endsWith stripped "}"))
      stripped
      (str (subs stripped 0 (dec len))
           ",\"timestamp\":\"" timestamp "\""
           "}"))))

(defxform 'assoc-time-and-strip-start
  (constantly assoc-time-and-strip-start))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main
(def search-pipeline
  '(pipeline
    (denorm-phase (read-phase (custom :assoc-time-and-strip-start)
                              (xforms :read-json))
                  (xform-phase (xforms :reify :denorm)))
    (schema-phase)
    (project-phase (delimiter "|"))))

(def-cli-pipeline search-pipeline)


;; (def raw (list "{\"a\": \"1\", \"b\": [\"2\", \"34\"]}" "{\"foo\": \"bazzle\"}" "{\"foo\": 123}"))
;; (with-in-str (reduce str (interpose "\n" (map str raw)))
;;   (-main "denorm")) ;; write to /tmp/denorm

;; (def d (list {"a" "1", "b_arr" "2", "b_idx" 0} {"a" "1", "b_arr" "34", "b_idx" 1} {"foo" "bazzle"} {"foo" 123}))
;; (with-in-str (reduce str (interpose "\n" (map str d)))
;;   (-main "schema")) ;; write to /tmp/sch

;; (-main "project" "/tmp/denorm" "/tmp/sch")

;;lein uberjar
;;cat /tmp/raw | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar denorm | tee /tmp/denorm | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar schema > /tmp/schema ; java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar project /tmp/denorm /tmp/schema > /tmp/project


;;CLEANUP

;; TODO delimiter at command line for project
;; TODO no-headers option for project
;; HEADER should be delimited by delimiter, and get ride of ( and )
;; TODO clojure.tools.cli library for cli args + options
;; look through rest of code

;;DEPLOY

;; figure out deploy strategy

;; at this point, push and publish v1.0.0

;;USE

;; create table for search data
;; daily quality checks for search data table
;; add jetl_version (from project.clj), j_processed_on, jetl_load_id (host + pid + time), jetl_impl (CLI)
;; cron job to poll s3 buckets, pipe s3cmd into this, pipe into vsql, cleanup the denorm and schema files after the load
