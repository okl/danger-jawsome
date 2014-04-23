(ns jawsome-cli.multi-pipeline-test
  "Tests for def-multi-cli-pipeline"
  {:author "Matt Halverson"
   :date "2014/04/23"}
  (:use clojure.test)
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-cli.core :refer [def-multi-cli-pipeline]]
            [jawsome-dsl.xform :refer [defvar]])
  (:require [jawsome-cli.core-test :refer [string->buffered-reader]]))

;; # Some preliminary definitions

(def m "{\"a\": 12, \"b\": 14, \"c\": 16}")

;; ## The pipelines

(def v1
  '(pipeline
    (denorm-phase
     (read-phase  (xforms :read-json))
     (xform-phase (custom :only ["a"])))))
(def v2
  '(pipeline
    (denorm-phase
     (read-phase  (xforms :read-json))
     (xform-phase (custom :remove ["a"])))))

;; ### Actual pipeline def

(def-multi-cli-pipeline
  ("v1" v1)
  ("v2" v2))

;; # TESTS!

(defn- process-with-version [map version]
  (let [s (with-out-str
            (-main version "denorm" "--input" (string->buffered-reader map)))]
    (read-string s)))

(deftest end-to-end-test
  (testing "Denorm phase"
    (is (= (process-with-version m "v1")
           {"a" 12}))
    (is (= (process-with-version m "v2")
           {"b" 14, "c" 16}))))
