(ns jawsome-cli.core-test
  "Tests for jawsome-cli.core!"
  {:author "Matt Halverson"
   :date "2014/03/31"}
  (:use clojure.test)
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-cli.core :refer [def-cli-pipeline]]))

(def simple-pipeline
  '(pipeline
    (denorm-phase
     (read-phase (xforms :read-json))
     (xform-phase (xforms :denorm)))
    (schema-phase)
    (project-phase)))

(def-cli-pipeline simple-pipeline)

(defn- string->buffered-reader [s]
  (clojure.java.io/reader (java.io.StringReader. s)))

(deftest end-to-end-test
  (let [raw-records (list "{\"a\": \"1\", \"b\": [\"2\", \"34\"]}"
                          "{\"foo\": \"bazzle\"}"
                          "{\"foo\": 123}")
        raw (reduce str (interpose "\n" (map str raw-records)))
        d (with-out-str
            (-main "denorm"
                   "--input"
                   (string->buffered-reader raw)))
        s (with-out-str
            (-main "schema"
                   "--input"
                   (string->buffered-reader d)))
        delimiter "|"
        p (with-out-str
            (-main "project"
                   "--input"
                   (string->buffered-reader d)
                   "--schema-path"
                   (string->buffered-reader s)
                   "--delimiter"
                   delimiter))]
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
          ["a|b_arr|b_idx|foo" "1|2|0|" "1|34|1|" "|||bazzle" "|||123"]))))
