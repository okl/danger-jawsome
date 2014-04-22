(ns jawsome-dsl.core-test
  "Tests for the Jawsome-dsl interpreter"
  {:author "Matt Halverson"
   :date "2014/03/31"}
  (:require [clojure.test :refer :all])
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jsonschema.type-system.types :as types])
  (:require [jawsome-dsl.core :refer [pipeline-interp
                                      interp-namespaced-pipeline
                                      default-env
                                      fields
                                      field-order]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize some constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def pipeline (pipeline-interp '(pipeline
                                 (denorm-phase (xform-phase (xforms :denorm)))
                                 (schema-phase)
                                 (project-phase))
                               default-env))
(def denorm (:denorm pipeline))
(def schema (:schema pipeline))
(def project (:project pipeline))

(def raw (list {"a" "1", "b" ["2" "34"]}
               {"foo" "bazzle"}
               {"foo" 123}))
(def d (denorm raw))
(def s (schema d))
(def header (field-order (fields s)))
(def what-header-should-be (list "a"
                                 "b_arr"
                                 "b_idx"
                                 "foo"))
(def p (map #(project % s "|") d))
(def what-p-should-be (list "1|2|0|"
                            "1|34|1|"
                            "|||bazzle"
                            "|||123"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest end-to-end-test
  (testing "Denorm-phase gets handled correctly in the top-level case"
    (is (= d (list {"a" "1", "b_arr" "2", "b_idx" 0}
                   {"a" "1", "b_arr" "34", "b_idx" 1}
                   {"foo" "bazzle"}
                   {"foo" 123}))))
  (testing "Schema-phase"
    (is (= s #jsonschema.type_system.types.Document{:properties #{"b_idx" "a" "b_arr" "foo"}, :map {"foo" #jsonschema.type_system.types.Union{:union-of #{#jsonschema.type_system.types.Str{:min 6, :max 6} #jsonschema.type_system.types.Int{:min 123, :max 123}}}, "a" #jsonschema.type_system.types.Str{:min 1, :max 1}, "b_arr" #jsonschema.type_system.types.Str{:min 1, :max 2}, "b_idx" #jsonschema.type_system.types.Int{:min 0, :max 1}}})))
  (testing "Project-phase"
    (is (= header what-header-should-be))
    (is (= p what-p-should-be))))

(deftest may-supply-your-own-fields-and-field-order
  (let [reversed (reverse header)]
    (testing "Sometimes we want to have the fields NOT be in alphabetical order"
      (is (= (map #(project % reversed "|" :sort-fields false) d)
             (list "|0|2|1"
                   "|1|34|1"
                   "bazzle|||"
                   "123|||"))))
    (testing "If sort-fields is true, then they should get alphabetized"
      (is (= (map #(project % reversed "|" :sort-fields true) d)
             what-p-should-be)))))

(deftest interp-namespaced-pipeline-works-for-syntax-quoted-pipelines
  (testing "Because it's convenient to use syntax-quote"
    (let [fns (interp-namespaced-pipeline
               `(pipeline (denorm-phase (xform-phase)))
               default-env)
          denorm-fn (:denorm fns)]
      (is (= (denorm-fn {})
             (list {}))))))

(deftest may-have-zero-or-one-of-each-phase
  (testing "There may either be zero or one"
    (testing "denorm phases"
      (is (thrown? RuntimeException
                   (pipeline-interp
                    '(pipeline
                      (denorm-phase (xform-phase))
                      (denorm-phase (xform-phase)))
                    default-env)))
      (is (thrown? RuntimeException
                   (pipeline-interp
                    '(pipeline
                      (denorm-phase (xform-phase))
                      (denorm-phase (xform-phase))
                      (denorm-phase (xform-phase)))
                    default-env)))
      (is (not (nil? (pipeline-interp
                      '(pipeline
                        (denorm-phase (xform-phase))
                        (schema-phase)
                        (project-phase))
                      default-env))))
      (is (not (nil? (pipeline-interp
                      '(pipeline
                        (schema-phase)
                        (project-phase))
                      default-env)))))
    (testing "schema phases"
      (is (thrown? RuntimeException
                   (pipeline-interp
                    '(pipeline
                      (schema-phase)
                      (schema-phase))
                    default-env)))
      (is (thrown? RuntimeException
                   (pipeline-interp
                    '(pipeline
                      (schema-phase)
                      (schema-phase)
                      (schema-phase))
                    default-env)))
      (is (not (nil? (pipeline-interp
                      '(pipeline
                        (schema-phase)
                        (project-phase))
                      default-env))))
      (is (not (nil? (pipeline-interp
                      '(pipeline
                        (denorm-phase (xform-phase))
                        (project-phase))
                      default-env)))))
    (testing "project phases"
      (is (thrown? RuntimeException
                   (pipeline-interp
                    '(pipeline
                      (project-phase)
                      (project-phase))
                    default-env)))
      (is (thrown? RuntimeException
                   (pipeline-interp
                    '(pipeline
                      (project-phase)
                      (project-phase)
                      (project-phase))
                    default-env)))
      (is (not (nil? (pipeline-interp
                      '(pipeline
                        (schema-phase)
                        (project-phase))
                      default-env))))
      (is (not (nil? (pipeline-interp
                      '(pipeline
                        (denorm-phase (xform-phase))
                        (schema-phase))
                      default-env)))))))
