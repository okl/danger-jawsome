(ns jawsome-dsl.core-test
  "Tests for the Jawsome top-level interpreter"
  {:author "Matt Halverson"
   :date "2014/02/25"}
  (:require [clojure.test :refer :all]
            [jawsome-dsl.core :refer [pipeline-interp
                                      default-env]]
            [jawsome-dsl.xform :refer [defvar
                                       defxform
                                       l1-interp
                                       xform-registry]]
            [roxxi.utils.print :refer [print-expr]]))

(deftest xforms-blocks-get-reordered
  (let [good '(xforms "Top-level"
                      (xforms "Xform phase"
                              (xforms "Xforms block"
                                      (xform (lookup hoist) (lookup hoist-cfg))
                                      (xform (lookup reify))
                                      (xform (lookup denorm)))))]
    (testing "Does the interpreter produce the correct output"
      (testing "when the xforms are already in order"
        (is (= (pipeline-interp
                '(pipeline
                  (xform-phase
                   (xforms :hoist (ref hoist-cfg) :reify :denorm)))
                default-env)
               good)))
      (testing "when the xforms are out of order"
        (is (= (pipeline-interp
                '(pipeline
                  (xform-phase
                   (xforms :hoist (ref hoist-cfg) :denorm :reify)))
                default-env)
               good))
        (is (= (pipeline-interp
                '(pipeline
                  (xform-phase
                   (xforms :denorm :hoist (ref hoist-cfg) :reify)))
                default-env)
               good))
        (is (= (pipeline-interp
                '(pipeline
                  (xform-phase
                   (xforms :reify :denorm :hoist (ref hoist-cfg))))
                default-env)
               good))))
    (testing "Can I have multiple xforms blocks to break the order"
      (is (= (pipeline-interp
              '(pipeline
                (xform-phase
                 (xforms :hoist (ref hoist-cfg) :reify :denorm)
                 (xforms :translate (ref foo))))
              default-env)
             '(xforms "Top-level"
                      (xforms "Xform phase"
                              (xforms "Xforms block"
                                      (xform (lookup hoist) (lookup hoist-cfg))
                                      (xform (lookup reify))
                                      (xform (lookup denorm)))
                              (xforms "Xforms block"
                                      (xform (lookup translate) (lookup foo))))))))))

(deftest custom-blocks-allowed
  (testing "I should be able to have xforms AND custom blocks"
    (is (= (pipeline-interp
            '(pipeline
              (read-phase (custom :my-fn)
                          (xforms :remove-cruft
                                  :recode-unicode
                                  :read-json))
              (xform-phase (custom :my-other-fn)
                           (xforms :hoist (ref hoist-cfg)
                                   :reify
                                   :translate (ref foo))
                           (custom :my-other-other-func)
                           (xforms :denorm)))
            default-env)
           '(xforms "Top-level"
                    (xforms "Read phase"
                            (xforms "Custom block"
                                    (xform (lookup my-fn)))
                            (xforms "Xforms block"
                                    (xform (lookup remove-cruft))
                                    (xform (lookup recode-unicode))
                                    (xform (lookup read-json))))
                    (xforms "Xform phase"
                            (xforms "Custom block"
                                    (xform (lookup my-other-fn)))
                            (xforms "Xforms block"
                                    (xform (lookup hoist) (lookup hoist-cfg))
                                    (xform (lookup reify))
                                    (xform (lookup translate) (lookup foo)))
                            (xforms "Custom block"
                                    (xform (lookup my-other-other-func)))
                            (xforms "Xforms block" (xform (lookup denorm)))))))))

(deftest ref-is-allowed
  (testing "Ref should get translated into lookup"
    (is (= (pipeline-interp
            '(pipeline (xform-phase (xforms :hoist (ref foo))))
            default-env)
           '(xforms "Top-level" (xforms "Xform phase" (xforms "Xforms block" (xform (lookup hoist) (lookup foo)))))))))

(deftest dethunk-is-allowed
  (testing "Dethunk should get translated into dethunk"
    (is (= (pipeline-interp
            '(pipeline
              (xform-phase (xforms :hoist (dethunk (ref yield-hoist-cfg)))))
            default-env)
           '(xforms "Top-level" (xforms "Xform phase" (xforms "Xforms block" (xform (lookup hoist) (dethunk (lookup yield-hoist-cfg))))))))))

(deftest big-fatty-but-realistic-pipeline
  (testing "Because I was using this to test in nrepl"
    (is (= (pipeline-interp
            '(pipeline
              (read-phase (xforms :remove-cruft
                                  :recode-unicode
                                  :read-json))
              (xform-phase (xforms
                            :hoist [{:properties ["nested_params" "X-Nested-Params"]
                                     :type "hoist-once-for-property"}
                                    {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"]
                                     :type "hoist-once-for-property"
                                     :prefix "exp_"
                                     :suffix "_test"}]
                            :remap-properties {"num" "renamed_field!"}
                            :reify
                            :translate {"-" nil}
                            :translate-paths {"no" false,
                                              "yes" true,
                                              "false" false,
                                              "true" true,
                                              0 false,
                                              1 true}
                            {["foo"] {"nine" 9 "nueve" 9}
                             ["bar" "sub_bar"] {"ten" "diez"}}
                            :type-enforce {["bool_prop_1"] :boolean
                                           ["bool_prop_2"] :boolean})
                           (custom
                            :static-values {"syn_prop" 42}
                            :static-values {"additional_prop" 4422}
                            :default-values {"syn_prop" 45
                                             "test_prop" 48}
                            :prune-nils)
                           (xforms :denorm)))
            default-env)
           '(xforms "Top-level"
                    (xforms "Read phase"
                            (xforms "Xforms block"
                                    (xform (lookup remove-cruft))
                                    (xform (lookup recode-unicode))
                                    (xform (lookup read-json))))
                    (xforms "Xform phase"
                            (xforms "Xforms block"
                                    (xform (lookup hoist) [{:properties ["nested_params" "X-Nested-Params"], :type "hoist-once-for-property"} {:properties ["nested_experiment_params" "X-Nested-Experiment-Params"], :type "hoist-once-for-property", :prefix "exp_", :suffix "_test"}])
                                    (xform (lookup remap-properties) {"num" "renamed_field!"})
                                    (xform (lookup reify))
                                    (xform (lookup translate) {"-" nil})
                                    (xform (lookup translate-paths) {"no" false, "yes" true, "false" false, "true" true, 0 false, 1 true} {["foo"] {"nine" 9, "nueve" 9}, ["bar" "sub_bar"] {"ten" "diez"}})
                                    (xform (lookup type-enforce) {["bool_prop_1"] :boolean, ["bool_prop_2"] :boolean}))
                            (xforms "Custom block"
                                    (xform (lookup static-values) {"syn_prop" 42})
                                    (xform (lookup static-values) {"additional_prop" 4422})
                                    (xform (lookup default-values) {"syn_prop" 45, "test_prop" 48})
                                    (xform (lookup prune-nils)))
                            (xforms "Xforms block"
                                    (xform (lookup denorm)))))))))


;; Why are there print-exprs in here? To force the evaluation of some lazy
;; sequences, which were otherwise lazily not throwing RuntimeExceptions :P
(deftest bad-xforms-in-xforms-block
  (testing "If an unrecognized xform appears in an ordered xforms block, an
exception should be thrown"
    (is (thrown?
         RuntimeException
         (print-expr
          (pipeline-interp
           '(pipeline
             (read-phase
              (xforms :jk-not-a-real-xform-lol)))
           default-env))))
    (is (thrown?
         RuntimeException
         (print-expr
          (pipeline-interp
           '(pipeline
             (xform-phase
              (xforms :just-one-xform-which-is-bad)))
           default-env))))
    (is (thrown?
         RuntimeException
         (print-expr
          (pipeline-interp
           '(pipeline
             (xform-phase
              (xforms :reify :bad-xform-name :denorm (ref hoist-cfg))
              (custom :shawns-fn)
              (xforms :translate (ref foo))))
           default-env))))
    (is (thrown?
         RuntimeException
         (print-expr
          (pipeline-interp
           '(pipeline
             (xform-phase
              (xforms :reify :hoist :bad-with-args (ref hoist-cfg))
              (custom :shawns-fn)))
           default-env))))
    (is (thrown?
         RuntimeException
         (print-expr
          (pipeline-interp
           '(pipeline
             (xform-phase
              (xforms :first-is-bad :hoist :denorm (ref hoist-cfg))
              (custom :shawns-fn)))
           default-env))))
    (is (thrown?
         RuntimeException
         (print-expr
          (pipeline-interp
           '(pipeline
             (xform-phase
              (xforms :reify :hoist :denorm (ref hoist-cfg) :last-is-bad)
              (custom :shawns-fn)))
           default-env)))))
  (testing "If an unrecognized xform appears in an un-ordered xforms block, no
exception should be thrown."
    (is (not (empty? (pipeline-interp
                      '(pipeline
                        (xform-phase
                         (xforms :reify :hoist :denorm (ref hoist-cfg))
                         (custom :shawns-fn)))
                      default-env))))
    (is (not (empty? (pipeline-interp
                      '(pipeline
                        (xform-phase
                         (xforms :reify :hoist :denorm (ref hoist-cfg))
                         (custom :this-can-be-a-bad-xform-name)))
                      default-env))))))




(deftest may-have-zero-or-one-read-phases
  (testing "Number of read phases may be"
    (testing "zero"
      (is (not (empty?
                (pipeline-interp
                 '(pipeline
                   (xform-phase (xforms :reify)))
                 default-env)))))
    (testing "one"
      (is (not (empty?
                (pipeline-interp
                 '(pipeline
                   (read-phase (xforms :remove-cruft))
                   (xform-phase (xforms :reify)))
                 default-env))))))
  (testing "May not have more than one read phase"
    (is (thrown?
         RuntimeException
         (pipeline-interp
          '(pipeline
            (read-phase (xforms :remove-cruft))
            (read-phase (xforms :unicode-recode))
            (xform-phase (xforms :reify)))
          default-env)))))

(deftest must-have-exactly-one-xform-phase
  (testing "Must have exactly one xform phase"
    (is (not (empty?
              (pipeline-interp
               '(pipeline
                 (read-phase (xforms :remove-cruft))
                 (xform-phase (xforms :reify)))
               default-env)))))
  (testing "Number of xform phases may not be"
    (testing "zero"
      (is (thrown?
           RuntimeException
           (pipeline-interp
            '(pipeline
              (read-phase (xforms :remove-cruft)))
            default-env))))
    (testing "one"
      (is (thrown?
           RuntimeException
           (pipeline-interp
            '(pipeline
              (read-phase (xforms :remove-cruft))
              (xform-phase (xforms :reify-values))
              (xform-phase (xforms :prune-nils)))
            default-env))))))

;; TODO uncomment this once project has been implemented
;; (deftest may-have-zero-or-one-project-phases
;;   (testing "Number of project phases may be"
;;     (testing "zero"
;;       (is (not (empty?
;;                 (pipeline-interp
;;                  '(pipeline
;;                    (xform-phase (xforms :reify-values)))
;;                  default-env)))))
;;     (testing "one"
;;       (is (not (empty?
;;                 (print-expr (pipeline-interp
;;                  '(pipeline
;;                    (xform-phase (xforms :reify-values))
;;                    (project-phase (xforms :some-keyword)))
;;                  default-env)))))))
;;   (testing "May not have more than one project phase"
;;     (is (thrown?
;;          RuntimeException
;;          (pipeline-interp
;;           '(pipeline
;;             (xform-phase (xforms :reify-values))
;;             (project-phase (xforms :some-keyword))
;;             (project-phase (xforms :other-keyword)))
;;           default-env)))))

(deftest end-to-end-test
  (testing "End-to-end test to see if we can convert an l2 to an l1 to
an actual clojure function for a pipeline with"
    (testing "just an xform phase"
      (let [l2 '(pipeline
                 (xform-phase (xforms :reify :denorm)))
            l1 (pipeline-interp l2 default-env)
            pipeline (l1-interp l1 (xform-registry))
            test-record {"flatten" {"me" "foobar"}
                         "denorm_prop" ["a" "b"]}]
        (is (= (pipeline test-record)
               (list {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "a",
                      "denorm_prop_idx" 0}
                     {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "b",
                      "denorm_prop_idx" 1})))))
    (testing "a read AND an xform phase"
      (let [l2 '(pipeline
                 (read-phase (xforms :read-json))
                 (xform-phase (xforms :reify :denorm)))
            l1 (pipeline-interp l2 default-env)
            pipeline (l1-interp l1 (xform-registry))
            test-record "{\"flatten\": {\"me\": \"foobar\"}, \"denorm_prop\": [\"a\", \"b\"]}"]
        (is (= (pipeline test-record)
               (list {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "a",
                      "denorm_prop_idx" 0}
                     {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "b",
                      "denorm_prop_idx" 1})))))))
