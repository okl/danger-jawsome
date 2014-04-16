(ns jawsome-dsl.denorm-test
  "Tests for the Jawsome denorm-phase interpreter"
  {:author "Matt Halverson"
   :date "2014/02/25"}
  (:require [clojure.test :refer :all])
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-dsl.denorm :refer [denorm-interp
                                        env-to-disable-post-denorm-cleanup]]
            [jawsome-dsl.xform :refer [defvar
                                       defxform
                                       l1-interp
                                       xform-registry]]))

(deftest xforms-blocks-get-reordered
  (let [good '(xforms "Top-level"
                      (xforms "Xform phase"
                              (xforms "Xforms block"
                                      (xform (lookup hoist) (lookup hoist-cfg))
                                      (xform (lookup reify))
                                      (xform (lookup denorm)))))]
    (testing "Does the interpreter produce the correct output"
      (testing "when the xforms are already in order"
        (is (= (denorm-interp
                '(denorm-phase
                  (xform-phase
                   (xforms :hoist (ref hoist-cfg) :reify :denorm)))
                env-to-disable-post-denorm-cleanup)
               good)))
      (testing "when the xforms are out of order"
        (is (= (denorm-interp
                '(denorm-phase
                  (xform-phase
                   (xforms :hoist (ref hoist-cfg) :denorm :reify)))
                env-to-disable-post-denorm-cleanup)
               good))
        (is (= (denorm-interp
                '(denorm-phase
                  (xform-phase
                   (xforms :denorm :hoist (ref hoist-cfg) :reify)))
                env-to-disable-post-denorm-cleanup)
               good))
        (is (= (denorm-interp
                '(denorm-phase
                  (xform-phase
                   (xforms :reify :denorm :hoist (ref hoist-cfg))))
                env-to-disable-post-denorm-cleanup)
               good))))
    (testing "Can I have multiple xforms blocks to break the order"
      (is (= (denorm-interp
              '(denorm-phase
                (xform-phase
                 (xforms :hoist (ref hoist-cfg) :reify :denorm)
                 (xforms :translate (ref foo))))
              env-to-disable-post-denorm-cleanup)
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
    (is (= (denorm-interp
            '(denorm-phase
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
            env-to-disable-post-denorm-cleanup)
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
    (is (= (denorm-interp
            '(denorm-phase (xform-phase (xforms :hoist (ref foo))))
            env-to-disable-post-denorm-cleanup)
           '(xforms "Top-level" (xforms "Xform phase" (xforms "Xforms block" (xform (lookup hoist) (lookup foo)))))))))

(deftest dethunk-is-allowed
  (testing "Dethunk should get translated into dethunk"
    (is (= (denorm-interp
            '(denorm-phase
              (xform-phase (xforms :hoist (dethunk (ref yield-hoist-cfg)))))
            env-to-disable-post-denorm-cleanup)
           '(xforms "Top-level" (xforms "Xform phase" (xforms "Xforms block" (xform (lookup hoist) (dethunk (lookup yield-hoist-cfg))))))))))

(deftest big-fatty-but-realistic-denorm-phase
  (testing "Because I was using this to test in nrepl"
    (is (= (denorm-interp
            '(denorm-phase
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
                            :log
                            :log debug
                            :static-values {"syn_prop" 42}
                            :static-values {"additional_prop" 4422}
                            :default-values {"syn_prop" 45
                                             "test_prop" 48}
                            :prune-nils)
                           (xforms :denorm)))
            env-to-disable-post-denorm-cleanup)
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
                                    (xform (lookup log))
                                    (xform (lookup log) "debug")
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
          (denorm-interp
           '(denorm-phase
             (read-phase
              (xforms :jk-not-a-real-xform-lol)))
           env-to-disable-post-denorm-cleanup))))
    (is (thrown?
         RuntimeException
         (print-expr
          (denorm-interp
           '(denorm-phase
             (xform-phase
              (xforms :just-one-xform-which-is-bad)))
           env-to-disable-post-denorm-cleanup))))
    (is (thrown?
         RuntimeException
         (print-expr
          (denorm-interp
           '(denorm-phase
             (xform-phase
              (xforms :reify :bad-xform-name :denorm (ref hoist-cfg))
              (custom :shawns-fn)
              (xforms :translate (ref foo))))
           env-to-disable-post-denorm-cleanup))))
    (is (thrown?
         RuntimeException
         (print-expr
          (denorm-interp
           '(denorm-phase
             (xform-phase
              (xforms :reify :hoist :bad-with-args (ref hoist-cfg))
              (custom :shawns-fn)))
           env-to-disable-post-denorm-cleanup))))
    (is (thrown?
         RuntimeException
         (print-expr
          (denorm-interp
           '(denorm-phase
             (xform-phase
              (xforms :first-is-bad :hoist :denorm (ref hoist-cfg))
              (custom :shawns-fn)))
           env-to-disable-post-denorm-cleanup))))
    (is (thrown?
         RuntimeException
         (print-expr
          (denorm-interp
           '(denorm-phase
             (xform-phase
              (xforms :reify :hoist :denorm (ref hoist-cfg) :last-is-bad)
              (custom :shawns-fn)))
           env-to-disable-post-denorm-cleanup)))))
  (testing "If an unrecognized xform appears in an un-ordered xforms block, no
exception should be thrown."
    (is (not (empty? (denorm-interp
                      '(denorm-phase
                        (xform-phase
                         (xforms :reify :hoist :denorm (ref hoist-cfg))
                         (custom :shawns-fn)))
                      env-to-disable-post-denorm-cleanup))))
    (is (not (empty? (denorm-interp
                      '(denorm-phase
                        (xform-phase
                         (xforms :reify :hoist :denorm (ref hoist-cfg))
                         (custom :this-can-be-a-bad-xform-name)))
                      env-to-disable-post-denorm-cleanup))))))




(deftest may-have-zero-or-one-read-phases
  (testing "Number of read phases may be"
    (testing "zero"
      (is (not (empty?
                (denorm-interp
                 '(denorm-phase
                   (xform-phase (xforms :reify)))
                 env-to-disable-post-denorm-cleanup)))))
    (testing "one"
      (is (not (empty?
                (denorm-interp
                 '(denorm-phase
                   (read-phase (xforms :remove-cruft))
                   (xform-phase (xforms :reify)))
                 env-to-disable-post-denorm-cleanup))))))
  (testing "May not have more than one read phase"
    (is (thrown?
         RuntimeException
         (denorm-interp
          '(denorm-phase
            (read-phase (xforms :remove-cruft))
            (read-phase (xforms :unicode-recode))
            (xform-phase (xforms :reify)))
          env-to-disable-post-denorm-cleanup)))))

(deftest must-have-exactly-one-xform-phase
  (testing "Must have exactly one xform phase"
    (is (not (empty?
              (denorm-interp
               '(denorm-phase
                 (read-phase (xforms :remove-cruft))
                 (xform-phase (xforms :reify)))
               env-to-disable-post-denorm-cleanup)))))
  (testing "Number of xform phases may not be"
    (testing "zero"
      (is (thrown?
           RuntimeException
           (denorm-interp
            '(denorm-phase
              (read-phase (xforms :remove-cruft)))
            env-to-disable-post-denorm-cleanup))))
    (testing "one"
      (is (thrown?
           RuntimeException
           (denorm-interp
            '(denorm-phase
              (read-phase (xforms :remove-cruft))
              (xform-phase (xforms :reify-values))
              (xform-phase (xforms :prune-nils)))
            env-to-disable-post-denorm-cleanup))))))

(deftest post-denorm-cleanup-xforms-only-get-added-once
  (testing "If we have multiple xforms blocks with an xform-phase, we still
only want the post-denorm cleanup xforms to get added once, after the final
defined xforms block"
    (is (= (denorm-interp '(denorm-phase (read-phase (xforms :read-json))
                                         (xform-phase (custom :foo)
                                                      (xforms :reify :denorm)
                                                      (custom :bar)
                                                      (xforms :hoist {}))) {})
           '(xforms
             "Top-level"
             (xforms
              "Read phase"
              (xforms "Xforms block" (xform (lookup read-json))))
             (xforms
              "Xform phase"
              (xforms "Custom block" (xform (lookup foo)))
              (xforms
               "Xforms block"
               (xform (lookup reify))
               (xform (lookup denorm)))
              (xforms "Custom block" (xform (lookup bar)))
              (xforms "Xforms block" (xform (lookup hoist) {}))
              (xforms
               "Added by default"
               (xform (lookup sanitize-field-names))
               (xform (lookup remove-empty-strings)))))))))

(deftest end-to-end-test
  (testing "End-to-end test to see if we can convert an l2 to an l1 to
an actual clojure function for a denorm-phase with"
    (testing "just an xform phase"
      (let [l2 '(denorm-phase
                 (xform-phase (xforms :reify :denorm)))
            l1 (denorm-interp l2 env-to-disable-post-denorm-cleanup)
            denorm-phase (l1-interp l1 (xform-registry))
            test-record {"flatten" {"me" "foobar"}
                         "denorm_prop" ["a" "b"]}]
        (is (= (denorm-phase test-record)
               (list {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "a",
                      "denorm_prop_idx" 0}
                     {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "b",
                      "denorm_prop_idx" 1})))))
    (testing "a read AND an xform phase"
      (let [l2 '(denorm-phase
                 (read-phase (xforms :read-json))
                 (xform-phase (xforms :reify :denorm)))
            l1 (denorm-interp l2 env-to-disable-post-denorm-cleanup)
            denorm-phase (l1-interp l1 (xform-registry))
            test-record "{\"flatten\": {\"me\": \"foobar\"}, \"denorm_prop\": [\"a\", \"b\"]}"]
        (is (= (denorm-phase test-record)
               (list {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "a",
                      "denorm_prop_idx" 0}
                     {"flatten_dot_me" "foobar",
                      "denorm_prop_arr" "b",
                      "denorm_prop_idx" 1})))))))
