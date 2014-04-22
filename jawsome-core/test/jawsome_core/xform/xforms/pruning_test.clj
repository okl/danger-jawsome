(ns jawsome-core.xform.xforms.pruning-test
  "Implements xform pipeline step: Value Synonym Mapping tests"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.pruning :refer [prune-nils
                                                       prune-paths
                                                       keep-paths]]))

(def sample {:a 5
             :b 6
             :c nil
             :d {:a 5
                 :b 6
                 :c nil
                 :d [1 2 "a" nil "b" nil]}
             :e {:a nil :b nil}})

(deftest prune-nils-test
  (testing "Testing nil pruning"
    (testing "pruning all of the paths, except for those within arrays. :e should be gone."
      (is (= (prune-nils sample)
             {:a 5, :b 6, :d {:a 5, :b 6, :d [1 2 "a" nil "b" nil]}})))))


(deftest prune-paths-test
  (let [m {:a true,
           :b {:sub_1 false,
               :sub_2 nil}}]
    (testing "Prune paths"
      (testing "works for paths that are present"
        (is (= (prune-paths m [[:b :sub_1]])
               {:a true, :b {:sub_2 nil}}))
        (is (= (prune-paths m [[:b]])
               {:a true}))
        (is (= (prune-paths m [[:a]])
               {:b {:sub_1 false, :sub_2 nil}}))
        (is (= (prune-paths m [:a])
               {:b {:sub_1 false, :sub_2 nil}}))
        (is (= (prune-paths m [:b])
               {:a true}))))
    (testing "does not fail for paths that aren't present"
      (is (= (prune-paths m [:not-present])
             m))
      (is (= (prune-paths m [[:not-present]])
             m)))))

(deftest keep-paths-test
  (let [m {:a true,
           :b {:sub_1 false,
               :sub_2 nil,
               :sub_3 {:sub_sub_k1 "sub_sub_value",
                       :sub_sub_k2 {}}}
           :c 1234,
           :d "never gonna give you up",
           :e ['you :got "rickrolled"]}]
    (testing "keep-paths"
      (testing "works for paths that are there"
        (is (= (keep-paths m [:a])
               {:a true}))
        (is (= (keep-paths m [[:a]])
               {:a true}))
        (is (= (keep-paths m [:a [:c]])
               {:c 1234, :a true}))
        (is (= (keep-paths m [:a [:b :sub_1] [:b :sub_3 :sub_sub_k2]])
               {:b {:sub_3 {:sub_sub_k2 {}},
                    :sub_1 false},
                :a true})))
      (testing "doesn't error if you ask it to keep paths that aren't there"
        (is (= (keep-paths m [:a :not_in_the_map [:also :not :in :the :map :like :for :sure]])
               {:a true}))
        (is (= (keep-paths m [:not_in_the_map])
               {}))
        (is (= (keep-paths m [])
               {}))))))
