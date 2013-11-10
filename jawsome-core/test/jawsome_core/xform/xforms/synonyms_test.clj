(ns jawsome-core.xform.xforms.synonyms-test
  "Implements xform pipeline step: Value Synonym Mapping tests"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.synonyms :refer [make-value-synonymizer]]))


(def synonyms
  {"null" nil
   "" nil
   "-" nil
   "yes" true
   "no" false
   "hallo" "hello"})

(def synonymizer (make-value-synonymizer synonyms))

(deftest synonymizer-test
  (testing "Testing synonymization"
    (testing "at the top level"
      (let [some-map {:a "null" :b nil
                      :c "-" :d ""
                      :e "yes" :f "no"
                      :g "hallo"}]
        (is (= (synonymizer some-map)
               {:a nil, :c nil, :b nil, :f false, :g "hello", :d nil, :e true}))))
    (testing "nested within the map"
      (let [some-map {:a "null"
                      :nested
                      {:b nil
                       :c "-" :d ""
                       :e "yes" :f "no"
                       :g "hallo"
                       :array ["-" "yes" "no" 6]}}]
        (is (= (synonymizer some-map)
               {:a nil,
                :nested {:c nil,
                         :b nil,
                         :f false,
                         :g "hello",
                         :d nil,
                         :array [nil true false 6],
                         :e true}}))))))
