(ns jawsome-dsl.xform-test
  "Tests for the Jawsome L1 interpreter"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [clojure.test :refer :all]
            [jawsome-dsl.xform :refer [defxform
                                       defvar
                                       l1-interp
                                       xform-registry]]
            [jawsome-dsl.init-registry]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set the stage
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def j "{\"url_encoded_thing\": \"http://domain.com/T4xGXfIqO3f6g',%20decoded='\\xaaf\\xbd\\x10]\\xc1\\x1d\\xa0\\x111\\xbf\\x9b\\xe0\\xb0\\x0f\\x15')\"}")
(def k "{\"num\": 1, \"num_as_str\": \"2\", \"str_prop\": \"this is a str\", \"bool_prop_1\": true, \"bool_prop_2\": \"this is not a bool\", \"array_prop\": [1, 2, 3], \"syn_prop\": \"-\"}")
(def l {"nested_params" {"hoist1" "foo" "hoist2" "bar"}
        "nested_experiment_params" {"hoist3" "baz"}
        "X-Nested-Params" {"hoist4" "foo" "hoist5" "bar"}
        "X-Nested-Experiment-Params" {"hoist6" "baz"}
        "shouldn't get hoisted" {"hoist7" "quux"}
        "denorm_prop" ["a" "b"]})
(def m {"foo" "nine"
        "bar" {"sub_bar" "ten"}
        "baz" "ten"})

(defvar 'hoist-cfg
  [{:properties ["nested_params" "X-Nested-Params"]
    :type "hoist-once-for-property"}
   {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"]
    :type "hoist-once-for-property"
    :prefix "exp_"
    :suffix "_test"}])

(defvar 'yield-type-enforcement
  (fn []
    {["bool_prop_1"] :boolean
     ["bool_prop_2"] :boolean}))

(def full-pipeline-program
  '(xforms
     (xforms "Read phase"
             (xform (lookup remove-cruft))
             (xform (lookup recode-unicode))
             (xform (lookup read-json)))
     (xforms "Xform phase"
             (xform (lookup hoist) (lookup hoist-cfg))
             (xform (lookup remap-properties) {"num" "renamed_field!"})
             (xform (lookup reify))
             (xform (lookup translate) {"-" nil})
             (xform (lookup translate-paths)
                    {"no" false,
                     "yes" true,
                     "false" false,
                     "true" true,
                     0 false,
                     1 true}
                    {["foo"] {"nine" 9 "nueve" 9}
                     ["bar" "sub_bar"] {"ten" "diez"}})
             (xform (lookup type-enforce) (dethunk (lookup yield-type-enforcement)))
             (xform (lookup static-values) {"syn_prop" 45})
             (xform (lookup static-values) {"additional_prop" 4422})
             (xform (lookup default-values) {"syn_prop" 42
                                             "test_prop" 48})
             (xform (lookup prune-nils))
             (xform (lookup denorm)))))
(def full-pipeline
  (l1-interp full-pipeline-program (xform-registry)))

(def no-read-phase-pipeline-program
  '(xforms "Xform phase"
             (xform (lookup hoist) (lookup hoist-cfg))
             (xform (lookup remap-properties) {"num" "renamed_field!"})
             (xform (lookup reify))
             (xform (lookup translate) {"-" nil})
             (xform (lookup translate-paths)
                    {"no" false,
                     "yes" true,
                     "false" false,
                     "true" true,
                     0 false,
                     1 true}
                    {["foo"] {"nine" 9 "nueve" 9}
                     ["bar" "sub_bar"] {"ten" "diez"}})
             (xform (lookup type-enforce) (dethunk (lookup yield-type-enforcement)))
             (xform (lookup static-values) {"syn_prop" 45})
             (xform (lookup static-values) {"additional_prop" 4422})
             (xform (lookup default-values) {"syn_prop" 42
                                             "test_prop" 48})
             (xform (lookup prune-nils))
             (xform (lookup denorm))))
(def no-read-phase-pipeline
  (l1-interp no-read-phase-pipeline-program (xform-registry)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Actual tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest end-to-end
  (testing "A pipeline can have a read and an xform phase"
    (is (= (full-pipeline j)
           (list {"url_encoded_thing" "http://domain.com/T4xGXfIqO3f6g',%20decoded='�f�]��1���')",
                  "additional_prop" 4422,
                  "syn_prop" 45,
                  "test_prop" 48})))
    (is (= (full-pipeline k)
           (list {"bool_prop_1" true,
                  "str_prop" "this is a str",
                  "num_as_str" 2,
                  "renamed_field!" 1,
                  "additional_prop" 4422,
                  "test_prop" 48,
                  "syn_prop" 45,
                  "array_prop_arr" 1,
                  "array_prop_idx" 0}
                 {"bool_prop_1" true,
                  "str_prop" "this is a str",
                  "num_as_str" 2,
                  "renamed_field!" 1,
                  "additional_prop" 4422,
                  "test_prop" 48,
                  "syn_prop" 45,
                  "array_prop_arr" 2,
                  "array_prop_idx" 1}
                 {"bool_prop_1" true,
                  "str_prop" "this is a str",
                  "num_as_str" 2,
                  "renamed_field!" 1,
                  "additional_prop" 4422,
                  "test_prop" 48,
                  "syn_prop" 45,
                  "array_prop_arr" 3,
                  "array_prop_idx" 2})))))


(deftest xform-phase-test
  (testing "A pipeline can have just an xform phase"
    (is (= (no-read-phase-pipeline l)
           (list {"denorm_prop_idx" 0,
                  "denorm_prop_arr" "a",
                  "shouldn't get hoisted_dot_hoist7" "quux",
                  "hoist1" "foo",
                  "hoist2" "bar",
                  "test_prop" 48,
                  "hoist4" "foo",
                  "hoist5" "bar",
                  "syn_prop" 45,
                  "additional_prop" 4422,
                  "exp_hoist6_test" "baz",
                  "exp_hoist3_test" "baz"}
                 {"denorm_prop_idx" 1,
                  "denorm_prop_arr" "b",
                  "shouldn't get hoisted_dot_hoist7" "quux",
                  "hoist1" "foo",
                  "hoist2" "bar",
                  "test_prop" 48,
                  "hoist4" "foo",
                  "hoist5" "bar",
                  "syn_prop" 45,
                  "additional_prop" 4422,
                  "exp_hoist6_test" "baz",
                  "exp_hoist3_test" "baz"})))
    (is (= (no-read-phase-pipeline m)
           (list {"bar_dot_sub_bar" "diez",
                  "baz" "ten",
                  "foo" 9,
                  "additional_prop" 4422,
                  "syn_prop" 45,
                  "test_prop" 48})))))

(defn my-custom-xform [m]
  (assoc m "I can insert" "custom values!"))

(defxform 'my-custom-xform
  (fn [] my-custom-xform))

(deftest custom-xforms-test
  (testing "I should be able to specify my own custom xforms"
    (let [pipeline
          (l1-interp
           '(xforms
             (xform (lookup reify))
             (xform (lookup static-values) {"syn_prop" 42})
             (xform (lookup my-custom-xform))
             (xform (lookup prune-nils)))
           (xform-registry))
          m {"a" "42", "b" nil}]
      (is (= (pipeline m)
             (list {"I can insert" "custom values!",
                    "syn_prop" 42,
                    "a" 42}))))))

(deftest empty-xforms-test
  (testing "May have empty xforms clause"
    (let [pipeline (l1-interp '(xforms) (xform-registry))]
      (is (= (pipeline m)
             (list m)))
      (is (= (pipeline {})
             (list {}))))))
