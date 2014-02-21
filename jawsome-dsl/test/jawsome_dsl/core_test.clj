(ns jawsome-dsl.core-test
  (:require [clojure.test :refer :all]
            ;; [jawsome-dsl.core :refer [pipeline-interp]]
            ))

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

;; (def full-pipeline
;;   (pipeline-interp
;;    '(pipeline
;;      (read-phase (xforms
;;                   :remove-cruft true
;;                   :unicode-recode true))
;;      (xform-phase (xforms
;;                    :hoist true [{:properties ["nested_params" "X-Nested-Params"]
;;                                  :type "hoist-once-for-property"}
;;                                 {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"]
;;                                  :type "hoist-once-for-property"
;;                                  :prefix "exp_"
;;                                  :suffix "_test"}]
;;                    :property-remapper true {"num" "renamed_field!"}
;;                    :reify-values true
;;                    :global-synonymizer true {"-" nil}
;;                    :path-specific-synonymizer true {"no" false,
;;                                                     "yes" true,
;;                                                     "false" false,
;;                                                     "true" true,
;;                                                     0 false,
;;                                                     1 true}
;;                                                    {["foo"] {"nine" 9 "nueve" 9}
;;                                                     ["bar" "sub_bar"] {"ten" "diez"}}
;;                    :value-type-filter true {["bool_prop_1"] :boolean
;;                                             ["bool_prop_2"] :boolean}
;;                    :static-value-merge false {"syn_prop" 42}
;;                    :static-value-merge true {"additional_prop" 4422}
;;                    :default-value-merge true {"syn_prop" 45
;;                                               "test_prop" 48}
;;                    :prune-nils true
;;                    :denormalize true)))))

;; (def no-read-phase-pipeline
;;   (pipeline-interp
;;    '(pipeline
;;      (xform-phase (xforms
;;                    :hoist true [{:properties ["nested_params" "X-Nested-Params"]
;;                                  :type "hoist-once-for-property"}
;;                                 {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"]
;;                                  :type "hoist-once-for-property"
;;                                  :prefix "exp_"
;;                                  :suffix "_test"}]
;;                    :property-remapper true {"num" "renamed_field!"}
;;                    :reify-values true
;;                    :global-synonymizer true {"-" nil}
;;                    :path-specific-synonymizer true {"no" false,
;;                                                     "yes" true,
;;                                                     "false" false,
;;                                                     "true" true,
;;                                                     0 false,
;;                                                     1 true}
;;                                                    {["foo"] {"nine" 9 "nueve" 9}
;;                                                     ["bar" "sub_bar"] {"ten" "diez"}}
;;                    :value-type-filter true {["bool_prop_1"] :boolean
;;                                             ["bool_prop_2"] :boolean}
;;                    :static-value-merge false {"syn_prop" 42}
;;                    :static-value-merge true {"additional_prop" 4422}
;;                    :default-value-merge true {"syn_prop" 45
;;                                               "test_prop" 48}
;;                    :prune-nils true
;;                    :denormalize true)))))

;; (deftest end-to-end
;;   (testing "A pipeline can have a read and an xform phase"
;;     (is (= (full-pipeline j)
;;            (list {"url_encoded_thing" "http://domain.com/T4xGXfIqO3f6g',%20decoded='�f�]��1���')",
;;                   "additional_prop" 4422,
;;                   "syn_prop" 45,
;;                   "test_prop" 48})))
;;     (is (= (full-pipeline k)
;;            (list {"bool_prop_1" true,
;;                   "str_prop" "this is a str",
;;                   "num_as_str" 2,
;;                   "renamed_field!" 1,
;;                   "additional_prop" 4422,
;;                   "test_prop" 48,
;;                   "array_prop_arr" 1,
;;                   "array_prop_idx" 0}
;;                  {"bool_prop_1" true,
;;                   "str_prop" "this is a str",
;;                   "num_as_str" 2,
;;                   "renamed_field!" 1,
;;                   "additional_prop" 4422,
;;                   "test_prop" 48,
;;                   "array_prop_arr" 2,
;;                   "array_prop_idx" 1}
;;                  {"bool_prop_1" true,
;;                   "str_prop" "this is a str",
;;                   "num_as_str" 2,
;;                   "renamed_field!" 1,
;;                   "additional_prop" 4422,
;;                   "test_prop" 48,
;;                   "array_prop_arr" 3,
;;                   "array_prop_idx" 2})))))


;; (deftest xform-phase-test
;;   (testing "A pipeline can have just an xform phase"
;;     (is (= (no-read-phase-pipeline l)
;;            (list {"denorm_prop_idx" 0,
;;                   "denorm_prop_arr" "a",
;;                   "shouldn't get hoisted_dot_hoist7" "quux",
;;                   "hoist1" "foo",
;;                   "hoist2" "bar",
;;                   "test_prop" 48,
;;                   "hoist4" "foo",
;;                   "hoist5" "bar",
;;                   "syn_prop" 45,
;;                   "additional_prop" 4422,
;;                   "exp_hoist6_test" "baz",
;;                   "exp_hoist3_test" "baz"}
;;                  {"denorm_prop_idx" 1,
;;                   "denorm_prop_arr" "b",
;;                   "shouldn't get hoisted_dot_hoist7" "quux",
;;                   "hoist1" "foo",
;;                   "hoist2" "bar",
;;                   "test_prop" 48,
;;                   "hoist4" "foo",
;;                   "hoist5" "bar",
;;                   "syn_prop" 45,
;;                   "additional_prop" 4422,
;;                   "exp_hoist6_test" "baz",
;;                   "exp_hoist3_test" "baz"})))
;;     (is (= (no-read-phase-pipeline m)
;;            (list {"bar_dot_sub_bar" "diez",
;;                   "baz" "ten",
;;                   "foo" 9,
;;                   "additional_prop" 4422,
;;                   "syn_prop" 45,
;;                   "test_prop" 48})))))



;; (deftest may-have-zero-or-one-read-phases
;;   (testing "Number of read phases may be"
;;     (testing "zero"
;;       (is (fn?
;;            (pipeline-interp
;;             '(pipeline
;;               (xform-phase (xforms :reify-values true)))))))
;;     (testing "one"
;;       (is (fn?
;;            (pipeline-interp
;;             '(pipeline
;;               (read-phase (xforms :remove-cruft true))
;;               (xform-phase (xforms :reify-values true))))))))
;;   (testing "May not have more than one read phase"
;;     (is (thrown?
;;          RuntimeException
;;          (pipeline-interp
;;           '(pipeline
;;             (read-phase (xforms :remove-cruft true))
;;             (read-phase (xforms :unicode-recode true))
;;             (xform-phase (xforms :reify-values true))))))))
;; (deftest must-have-exactly-one-xform-phase
;;   (testing "Must have exactly one xform phase"
;;     (is (fn?
;;          (pipeline-interp
;;           '(pipeline
;;             (read-phase (xforms :remove-cruft true))
;;             (xform-phase (xforms :reify-values true)))))))
;;   (testing "Number of xform phases may not be"
;;     (testing "zero"
;;       (is (thrown?
;;            RuntimeException
;;            (pipeline-interp
;;             '(pipeline
;;               (read-phase (xforms :remove-cruft true)))))))
;;     (testing "one"
;;       (is (thrown?
;;            RuntimeException
;;            (pipeline-interp
;;             '(pipeline
;;               (read-phase (xforms :remove-cruft true))
;;               (xform-phase (xforms :reify-values true))
;;               (xform-phase (xforms :prune-nils true)))))))))

;; (deftest may-have-zero-or-one-project-phases
;;   (testing "Number of project phases may be"
;;     (testing "zero"
;;       (is (fn?
;;            (pipeline-interp
;;             '(pipeline
;;               (xform-phase (xforms :reify-values true)))))))
;;     (testing "one"
;;       (is (fn?
;;            (pipeline-interp
;;             '(pipeline
;;               (xform-phase (xforms :reify-values true))
;;               (project-phase (xforms :some-keyword true))))))))
;;   (testing "May not have more than one project phase"
;;     (is (thrown?
;;          RuntimeException
;;          (pipeline-interp
;;           '(pipeline
;;             (xform-phase (xforms :reify-values true))
;;             (project-phase (xforms :some-keyword true))
;;             (project-phase (xforms :other-keyword true))))))))

;; ;; TODO should this be (list m) or just m?
;; (deftest may-have-empty-xforms
;;   (testing "May have empty xforms clause"
;;     (let [pass-through (pipeline-interp
;;                         '(pipeline
;;                           (xform-phase (xforms))))]
;;       (is (= (pass-through m)
;;              m)))))
