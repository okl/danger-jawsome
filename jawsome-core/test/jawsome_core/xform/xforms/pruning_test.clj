(ns jawsome-core.xform.xforms.pruning-test
  "Implements xform pipeline step: Value Synonym Mapping tests"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.pruning :refer [prune-nils]]))


(def sample {:a 5
             :b 6
             :c nil
             :d {:a 5
                 :b 6
                 :c nil
                 :d [1 2 "a" nil "b" nil]}
             :e {:a nil :b nil}})

(deftest pruning-test
  (testing "Testing nil pruning"
    (testing "pruning all of the paths, except for those within arrays. :e should be gone."
      (is (= (prune-nils sample) 
             {:a 5, :b 6, :d {:a 5, :b 6, :d [1 2 "a" nil "b" nil]}})))))
