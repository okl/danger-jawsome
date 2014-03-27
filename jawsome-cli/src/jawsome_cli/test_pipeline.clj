(ns jawsome-cli.test-pipeline
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-cli.core :refer [def-cli-pipeline]])
  (:require [jawsome-dsl.xform :refer [defxform]]))

;;TODO this is the beginnings of jawsome-pipeline-searchpro
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO is this unit-test-worthy?

;; (def raw (list "{\"a\": \"1\", \"b\": [\"2\", \"34\"]}" "{\"foo\": \"bazzle\"}" "{\"foo\": 123}"))
;; (with-in-str (reduce str (interpose "\n" (map str raw)))
;;   (-main "denorm")) ;; write to /tmp/denorm

;; (def d (list {"a" "1", "b_arr" "2", "b_idx" 0} {"a" "1", "b_arr" "34", "b_idx" 1} {"foo" "bazzle"} {"foo" 123}))
;; (with-in-str (reduce str (interpose "\n" (map str d)))
;;   (-main "schema")) ;; write to /tmp/sch

;; (-main "project" "/tmp/denorm" "/tmp/sch")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; USAGE

;;lein uberjar; cat /tmp/raw | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar denorm | tee /tmp/denorm | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar schema > /tmp/schema ; java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar project /tmp/denorm /tmp/schema

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ROADMAP

;;CLEANUP

;; TODO first, hook into clojure.tools.cli library for cli args + options
;; TODO delimiter at command line for project phase
;; TODO the header should be delimited by delimiter, and get rid of ( and )
;; TODO no-headers option at command line for project phase (instead, you
;;      specify the desired output path of the header file)
;; TODOs from through rest of code
;;    add unit tests from jawsome_dsl/core.clj
;;    change the old jawsome-dsl.core/pipeline-interp to be denorm-interp,
;;         update all the existing jawsome_dsl unit tests to use denorm-interp (?)
;;    revisit separate_phases... read+xform are now denorm; project may not need to be considered
;; TODO allow specific file names rather than always reading from *in*, writing to *out*
;;    (optional -- default is to read from *in* and write to *out*

;; TODO (down the road) allow denorm+schema to happen together as a performance optimization



;; the ... should be able to support arbitrary args supplied by the caller of the program, and we
;; need to give them the ability to layer in their own preflight and postflight functions to be called
;; around the invocations in main. This can be part of the def-cli etc...
;; these functions might intialize DB stuff, loggers, etc.
;;
;; look at clojure-hadoop to see how this type of extensibility is generated through
;; its configuration of defjob (https://github.com/alexott/clojure-hadoop/blob/master/src/clojure_hadoop/defjob.clj)
;; if someone wants to combine xform + schema for the sake of performance, they should be able
;; to do that by specifying it in the '(pipeline ...) and then passing in a command-line option
;; to *1, where they specify the schema-output-path (schema.clj in the above)



;; (def my-pipeline
;;   '(pipeline
;;     (xform-phase ...)))
;; (def-cli-pipeline
;;   :pipeline my-pipeline
;;   :log-to "/an/absolute/path"
;;   :db-cnxn "path/to/yml/file")


;;DEPLOY

;; figure out deploy strategy

;; at this point, push and publish v1.0.0

;;USE

;; create table for search data
;; daily quality checks for search data table
;; add jetl_version (from project.clj), j_processed_on, jetl_load_id (host + pid + time), jetl_impl (CLI)
;; cron job to poll s3 buckets, pipe s3cmd into this, pipe into vsql, cleanup the denorm and schema files after the load
