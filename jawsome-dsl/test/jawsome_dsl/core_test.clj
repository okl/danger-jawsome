(ns jawsome-dsl.core-test
  (:require [clojure.test :refer :all]
            [jawsome-dsl.core :refer [pipeline-interp]]
            ))

(deftest may-have-zero-or-one-read-phases
  (testing "Number of read phases may be"
    (testing "zero"
      (is (fn?
           (pipeline-interp
            '(pipeline
              (xform-phase (xforms :reify-values true)))))))
    (testing "one"
      (is (fn?
           (pipeline-interp
            '(pipeline
              (read-phase (xforms :remove-cruft true))
              (xform-phase (xforms :reify-values true))))))))
  (testing "May not have more than one read phase"
    (is (thrown?
         RuntimeException
         (pipeline-interp
          '(pipeline
            (read-phase (xforms :remove-cruft true))
            (read-phase (xforms :unicode-recode true))
            (xform-phase (xforms :reify-values true))))))))
(deftest must-have-exactly-one-xform-phase
  (testing "Must have exactly one xform phase"
    (is (fn?
         (pipeline-interp
          '(pipeline
            (read-phase (xforms :remove-cruft true))
            (xform-phase (xforms :reify-values true)))))))
  (testing "Number of xform phases may not be"
    (testing "zero"
      (is (thrown?
           RuntimeException
           (pipeline-interp
            '(pipeline
              (read-phase (xforms :remove-cruft true)))))))
    (testing "one"
      (is (thrown?
           RuntimeException
           (pipeline-interp
            '(pipeline
              (read-phase (xforms :remove-cruft true))
              (xform-phase (xforms :reify-values true))
              (xform-phase (xforms :prune-nils true)))))))))

(deftest may-have-zero-or-one-project-phases
  (testing "Number of project phases may be"
    (testing "zero"
      (is (fn?
           (pipeline-interp
            '(pipeline
              (xform-phase (xforms :reify-values true)))))))
    (testing "one"
      (is (fn?
           (pipeline-interp
            '(pipeline
              (xform-phase (xforms :reify-values true))
              (project-phase (xforms :some-keyword true))))))))
  (testing "May not have more than one project phase"
    (is (thrown?
         RuntimeException
         (pipeline-interp
          '(pipeline
            (xform-phase (xforms :reify-values true))
            (project-phase (xforms :some-keyword true))
            (project-phase (xforms :other-keyword true))))))))
