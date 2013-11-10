(ns jawsome-core.xform.xforms.static-injection-test
  "Tests xform pipeline step: Static Value Injection"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.static-injection :refer :all]))


(def static-map
  {:a 1, :b 2, :c 3, :d 4,
   :nested {:a 10, :b 20, :c 30, :d 40}
   :2nested {:1nested {:a 10, :b 20, :c 30, :d 40}}})

;; default-value-merge-fn

(deftest static-value-merge-fn-test
  (testing "Testing static-value-merge-fn"
    (testing "non-recursive merging"
      (let [merge-fn (static-value-merge-fn static-map)]
        (testing "overwriting values (x and y should disappear)"
          (let [test-map {:a "1"
                          :nested {:a "10", :b "20" :x "x", :y "y"}
                          :2nested {:1nested {:a "10", :b "20", :x "x", :y "y"}}}]
            (is (= (merge-fn test-map)
                   (merge test-map static-map)))))
        (testing "empty map"
          (let [test-map {}]
            (is (= (merge-fn test-map)
                   static-map))))
        (testing "non-overlapping set of keys"
          (let [test-map {:x "x"  :y "y"}]
            (is (= (merge-fn test-map)
                   (merge test-map static-map)))))))
    (testing "recursive merging"
      ;; note the "true" here to enable recursive merging
      (let [merge-fn (static-value-merge-fn static-map true)]
        (testing "overwriting values (x and y shouldn't disappear)"
          (let [test-map {:a "1"
                          :nested {:a "10", :b "20" :x "x", :y "y"}
                          :2nested {:1nested {:a "10", :b "20", :x "x", :y "y"}}}]
            (is (= (merge-fn test-map)
                   {:2nested {:1nested {:y "y", :x "x", :a 10, :c 30, :b 20, :d 40}},
                    :a 1,
                    :c 3,
                    :b 2,
                    :d 4,
                    :nested {:y "y", :x "x", :a 10, :c 30, :b 20, :d 40}}))))
        (testing "empty map"
          (let [test-map {}]
            (is (= (merge-fn test-map)
                   static-map))))
        (testing "non-overlapping set of keys"
          (let [test-map {:x "x"  :y "y"}]
            (is (= (merge-fn test-map)
                   (merge test-map static-map)))))))))


(def default-map
  {:a 1, :b 2, :c 3, :d 4,
   :nested {:a 10, :b 20, :c 30, :d 40}
   :2nested {:1nested {:a 10, :b 20, :c 30, :d 40}}})

(deftest default-value-merge-fn-test
  (testing "Testing default-value-merge-fn"
    (testing "non-recursive merging"
      (let [merge-fn (default-value-merge-fn default-map)]
        (testing "not overwriting supplied values -- (nested.c&d, and 2nested.1nested.c&d shouldn't appear because this isn't recursive, and a shouldn't be replaced however b,c,d should all be supplied)"
          (let [test-map {:a "1"
                          :nested {:a "10", :b "20" :x "x", :y "y"}
                          :2nested {:1nested {:a "10", :b "20", :x "x", :y "y"}}}]
            (is (= (merge-fn test-map)
                   {:2nested {:1nested {:y "y", :x "x", :a "10", :b "20"}}, 
                    :a "1", :c 3, :b 2, :d 4, 
                    :nested {:y "y", :x "x", :a "10", :b "20"}}))))
        (testing "not overwriting supplied values -- we shouldn't see 2nested.1nested overwritten here"
          (let [test-map {:a "1"
                          :nested {:a "10", :b "20" :x "x", :y "y"}
                          :2nested {:1nested "something else"}}]
            (is (= (merge-fn test-map)
                   {:2nested {:1nested "something else"},
                    :a "1", :c 3, :b 2, :d 4, 
                    :nested {:y "y", :x "x", :a "10", :b "20"}}))))
        (testing "empty map - all defaults should be supplied"
          (let [test-map {}]
            (is (= (merge-fn test-map)
                   default-map))))
        (testing "non-overlapping set of keys"
          (let [test-map {:x "x"  :y "y"}]
            (is (= (merge-fn test-map)
                   (merge default-map test-map)))))))
    (testing "recursive merging"
      ;; note the "true" here to enable recursive merging
      (let [merge-fn (default-value-merge-fn default-map true)]
        (testing "not overwriting supplied values -- nested.c&d, and 2nested.1nested.c&d should appear because this is recursive, and a shouldn't be replaced however b,c,d should all be supplied"
          (let [test-map {:a "1"
                          :nested {:a "10", :b "20" :x "x", :y "y"}
                          :2nested {:1nested {:a "10", :b "20", :x "x", :y "y"}}}]
            (is (= (merge-fn test-map)
                   {:2nested {:1nested {:y "y", :x "x", :a "10", :c 30, :b "20", :d 40}},
                    :a "1", :c 3, :b 2, :d 4,
                    :nested {:y "y", :x "x", :a "10", :c 30, :b "20", :d 40}}))))
        (testing "not overwriting supplied values -- we shouldn't see 2nested.1nested overwritten here because a value was specified- and even though there were default paths spcified for 1 nested, we shouldn't see them appear because the initial value isn't a map, and therefore we shouldn't recursively apply defaults since they're not appropriate. nested.x and nested.y, however, should appear since they were omitted, and b,c,d also should still appear"
          (let [test-map {:a "1"
                          :nested {:a "10", :b "20" :x "x", :y "y"}
                          :2nested {:1nested "something else"}}]
            (is (= (merge-fn test-map)
                   {:2nested {:1nested "something else"}, 
                    :a "1", :c 3, :b 2, :d 4, 
                    :nested {:y "y", :x "x", :a "10", :c 30, :b "20", :d 40}}))))
        (testing "empty map"
          (let [test-map {}]
            (is (= (merge-fn test-map)
                   default-map))))
        (testing "non-overlapping set of keys"
          (let [test-map {:x "x"  :y "y"}]
            (is (= (merge-fn test-map)
                   (merge test-map default-map)))))))))
