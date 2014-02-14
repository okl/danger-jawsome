(ns jawsome-core.xform.xforms.synonyms-test
  "Implements xform pipeline step: Value Synonym Mapping tests"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.synonyms :refer [make-value-synonymizer
                                                        path-specific-synonymizer]]))


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

(def test-map
  {"foo" "nine"
   "bar" {"sub_bar" "ten" "other_sub_bar" "ten"}
   "baz" "ten"
   "should_be_translated" 1
   "shouldnt_be_translated" 1
   "overrideable?" 0})

(def default-syns
  {"no" false,
   "yes" true,
   "false" false,
   "true" true,
   0 false,
   1 true})

(def path=>extra-syns
  {["foo"] {"nine" 9 "nueve" 9}
   ["bar" "sub_bar"] {"ten" "diez"}
   ["should_be_translated"] {}
   ["overrideable?"] {0 "overridden!"}})

(deftest path-specific-synonymizer-test
  (testing "Testing path-specific synonymization"
    (testing "everything at once"
      (is (= (path-specific-synonymizer test-map default-syns path=>extra-syns)
             {"foo" 9,
              "should_be_translated" true,
              "shouldnt_be_translated" 1,
              "bar" {"sub_bar" "diez" "other_sub_bar" "ten"},
              "overrideable?" "overridden!",
              "baz" "ten"})))
    (testing "a path may have multiple synonyms"
      (is (= (path-specific-synonymizer {"foo" "nine"} default-syns path=>extra-syns)
             {"foo" 9}))
      (is (= (path-specific-synonymizer {"foo" "nueve"} default-syns path=>extra-syns)
             {"foo" 9})))))
