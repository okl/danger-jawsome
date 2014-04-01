(ns jawsome-core.xform.xforms.post-denorm-cleanup-test
  "Tests for jawsome-core.xform.xforms.post-denorm-cleanup"
  {:author "Matt Halverson"
   :date "2014/03/31"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.post-denorm-cleanup
             :refer [sanitize-field-names
                     remove-empty-string-fields]]))

(def sample {"funky.separators:in=here" nil
             "spaces here" nil
             "UpPeRcAsE iS oBnOxIoUs" nil
             "empty" ""
             "also_empty" ""})

(deftest sanitize-test
  (testing "Testing sanitize-field-names"
    (is (= (sanitize-field-names sample)
           {"uppercase_is_obnoxious" nil,
            "spaces_here" nil,
            "empty" "",
            "also_empty" "",
            "funky_separators_in_here" nil}))))

(deftest remove-empty-string-fields-test
  (testing "Testing remove-empty-string-fields"
    (is (= (remove-empty-string-fields sample)
           {"UpPeRcAsE iS oBnOxIoUs" nil,
            "spaces here" nil,
            "funky.separators:in=here" nil}))))
