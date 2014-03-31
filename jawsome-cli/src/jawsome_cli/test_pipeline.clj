(ns jawsome-cli.test-pipeline
  "Code to make a CLI wrapper around jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-cli.core :refer [def-cli-pipeline]])
  (:require [jawsome-dsl.xform :refer [defxform]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; USAGE

;;lein uberjar; cat /tmp/raw | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar denorm | tee /tmp/denorm | java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar schema > /tmp/schema ; java -jar /Users/mhalverson/Code/okl/danger-jawsome/jawsome-cli/target/jawsome-cli-0.1.0-SNAPSHOT-standalone.jar project --input /tmp/denorm --schema-path /tmp/schema --output /tmp/blat

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ROADMAP

;;CLEANUP

;;DEPLOY

;; figure out deploy strategy

;; at this point, push and publish v1.0.0 for jawsome-core, jawsome-dsl, jawsome-cli

;;USE

;; create table for search data
;; daily quality checks for search data table
;; add jetl_version (from project.clj), j_processed_on, jetl_load_id (host + pid + time), jetl_impl (CLI)
;; cron job to poll s3 buckets, pipe s3cmd into this, pipe into vsql, cleanup the denorm and schema files after the load
