(ns jawsome-dsl.core-test
  "Tests for the Jawsome-dsl interpreter"
  {:author "Matt Halverson"
   :date "2014/03/31"}
  (:require [clojure.test :refer :all])
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jsonschema.type-system.types :as types])
  (:require [jawsome-dsl.core :refer [pipeline-interp
                                      default-env
                                      field-order]]))

(deftest end-to-end-test
  (let [p (pipeline-interp '(pipeline
                             (denorm-phase (xform-phase (xforms :denorm)))
                             (schema-phase)
                             (project-phase))
                           {})
        denorm (:denorm p)
        schema (:schema p)
        project (:project p)
        raw (list {"a" "1", "b" ["2" "34"]}
                  {"foo" "bazzle"}
                  {"foo" 123})
        d (denorm raw)
        s (schema d)
        header (field-order s)
        p (map #(project % s "|") d)]
    (testing "Denorm-phase gets handled correctly in the top-level case"
      (is (= d (list {"a" "1", "b_arr" "2", "b_idx" 0}
                     {"a" "1", "b_arr" "34", "b_idx" 1}
                     {"foo" "bazzle"}
                     {"foo" 123}))))
    (testing "Schema-phase"
      (is (= s #jsonschema.type_system.types.Document{:properties #{"b_idx" "a" "b_arr" "foo"}, :map {"foo" #jsonschema.type_system.types.Union{:union-of #{#jsonschema.type_system.types.Str{:min 6, :max 6} #jsonschema.type_system.types.Int{:min 123, :max 123}}}, "a" #jsonschema.type_system.types.Str{:min 1, :max 1}, "b_arr" #jsonschema.type_system.types.Str{:min 1, :max 2}, "b_idx" #jsonschema.type_system.types.Int{:min 0, :max 1}}})))
    (testing "Project-phase"
      (is (= header (list "a"
                          "b_arr"
                          "b_idx"
                          "foo")))
      (is (= p (list "1|2|0|"
                     "1|34|1|"
                     "|||bazzle"
                     "|||123"))))))

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
