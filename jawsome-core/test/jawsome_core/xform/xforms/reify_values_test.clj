(ns jawsome-core.xform.xforms.reify-values-test
  "Implements xform pipeline step: String Value Reification tests"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [clojure.test :refer :all])
  (:require [cheshire.core :refer [parse-string]])
  (:require [jawsome-core.xform.xforms.reify-values :refer :all]))



(deftest reify-values-test
  (testing "Testing the basic parsing assertions: "
    (testing "Number"
      (is (= (reify-values {:a "5"}) {:a 5})))
    (testing "true/false/null"
      (is (= (reify-values {:a "true"}) {:a true}))
      (is (= (reify-values {:a "false"}) {:a false}))
      (is (= (reify-values {:a "null"}) {:a nil})))
    (testing "Array"
      (is (= (reify-values {:a "[5, 6, 7]"}) {:a [5, 6, 7]})))
    (testing "Map"
      (is (= (reify-values {:a "{\"a\":\"5\", \"b\":\"6\", \"c\":\"7\"}"})
             {:a {"a" 5, "b" 6, "c" 7}})))
    (testing "Parsed"
      (is (= (reify-values {:a true :b nil :c false :d 6})
             {:a true :b nil :c false :d 6})))))

(defn resource [file-name]
  (str "test/jawsome_core/resources/xform/xforms/reify_values/" file-name))

(deftest test-nested-escaped-structure []
  (let [nested-json-string (slurp (resource "nested-escaped-json.js"))
        base-json (parse-string nested-json-string)
        reified (reify-values base-json)]
    (is (= reified
           {"level1" {"level2_0" [{"level3_i0_p0_key" 1, "level3_i1_p1_key" 10}
                                  {"level3_i1_p0_key" 2, "level3_i1_p1_key" 20}
                                  {"level3_i2_p0_key" 3, "level3_i2_p1_key" 1}],
                      "level2_1" 18824,
                      "level2_2" 906463}}))))

(deftest test-another-nested-escaped-structure []
  (let [nested-json-string (slurp (resource "nested-escaped-json2.js"))
        base-json (parse-string nested-json-string)
        reified (reify-values base-json)]
    (is (= reified
           {"level1" {"level2_0" 1,
                      "level2_1" 0,
                      "level2_2" [{"level2_i0_p0_key" 1, "level2_i1_p0_key" 1}]}}))))

(deftest test-max-integer []
  (is (= (reify-values (parse-string "{\"a\" : \"9223372036854775807\"}"))
         {"a" 9223372036854775807})))

;; We don't want numbers to tip over the 64-bit threshold for longs.
;; If a number is that big, we'll just assume it's a string.
(deftest test-really-huge-integer []
  (is (= (reify-values (parse-string "{\"a\" : \"89844588526466531663538850397115\"}"))
         {"a" "89844588526466531663538850397115"})))

(deftest test-really-huge-decimal []
  (is (= (reify-values (parse-string "{\"a\" : \"89844588526466531663538850397115.8984458852646650397115\"}"))
         {"a" 8.984458852646652E31})))

(deftest test-number-starting-with-a-zero []
  (is (= (reify-values (parse-string "{\"a\" : \"00012234\"}"))
         {"a" "00012234"})))

(deftest test-a-big-url []
  (is (= (reify-values (parse-string "{\"a\" : \"https://www.google.com/foo/20712?utm_source=Daily&utm_medium=Email&utm_campaign=23405&utm_content=3/21/2013.2553960&utm_term=New_Car.Image.2.20712\"}"))
         {"a" "https://www.google.com/foo/20712?utm_source=Daily&utm_medium=Email&utm_campaign=23405&utm_content=3/21/2013.2553960&utm_term=New_Car.Image.2.20712"})))
