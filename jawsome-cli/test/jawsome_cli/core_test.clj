(ns jawsome-cli.core-test
  "Tests for jawsome-cli.core!"
  {:author "Matt Halverson"
   :date "2014/03/31"}
  (:use clojure.test)
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-cli.core :refer [def-cli-pipeline]]))

;; # Some preliminary definitions

;; ## The pipeline
(def simple-pipeline
  '(pipeline
    (denorm-phase
     (read-phase (xforms :read-json))
     (xform-phase (xforms :denorm)))
    (schema-phase)
    (project-phase)))

(def-cli-pipeline simple-pipeline)

;; ## Helpers

(defn- string->buffered-reader [s]
  (clojure.java.io/reader (java.io.StringReader. s)))

;; ## Sample data
(def raw-records
  (list "{\"a\": \"1\", \"b\": [\"2\", \"34\"]}"
        "{\"foo\": \"bazzle\"}"
        "{\"foo\": 123}"))
(def raw
  (reduce str (interpose "\n" (map str raw-records))))
(def d
  (with-out-str
    (-main "denorm"
           "--input"
           (string->buffered-reader raw))))
(def s
  (with-out-str
    (-main "schema"
           "--input"
           (string->buffered-reader d))))
(def delimiter "|")
(def p
  (with-out-str
    (-main "project"
           "--input"
           (string->buffered-reader d)
           "--schema"
           (string->buffered-reader s)
           "--delimiter"
           delimiter)))

;; # TESTS!

(deftest end-to-end-test
  (testing "Denorm phase"
    (is (= (map read-string (clojure.string/split d #"\n"))
           (list {"a" "1", "b_arr" "2", "b_idx" 0}
                 {"a" "1", "b_arr" "34", "b_idx" 1}
                 {"foo" "bazzle"}
                 {"foo" 123}))))
  (testing "Schema phase"
    (is (= (read-string s)
           #jsonschema.type_system.types.Document{:properties #{"b_idx" "a" "b_arr" "foo"}, :map {"foo" #jsonschema.type_system.types.Union{:union-of #{#jsonschema.type_system.types.Str{:min 6, :max 6} #jsonschema.type_system.types.Int{:min 123, :max 123}}}, "a" #jsonschema.type_system.types.Str{:min 1, :max 1}, "b_arr" #jsonschema.type_system.types.Str{:min 1, :max 2}, "b_idx" #jsonschema.type_system.types.Int{:min 0, :max 1}}})))
  (testing "Project phase"
    (is (= (clojure.string/split p #"\n"))
        ["a|b_arr|b_idx|foo" "1|2|0|" "1|34|1|" "|||bazzle" "|||123"])))

(deftest must-specify-schema
  (testing "It is an error not to specify the --schema in the project phase"
    (is (thrown? RuntimeException
                 (-main "project"
                        "--input"
                        (string->buffered-reader d)
                        ;; "--schema"
                        ;; (string->buffered-reader s)
                        "--delimiter"
                        delimiter)))))
