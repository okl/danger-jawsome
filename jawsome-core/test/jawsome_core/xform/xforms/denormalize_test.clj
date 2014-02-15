(ns jawsome-core.xform.xforms.denormalize-test
  "Implements xform pipeline step: Denormalize tests"
  {:author "Matt Halverson"
   :date "2/14/2014"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.denormalize :refer [denormalize]])
  (:require [roxxi.utils.print :refer [print-expr]]))

(deftest denormalize-test
  (testing "Arrays should get denormalized"
    (is (= (denormalize {"array_prop" ["a" "b"]})
           (list {"array_prop_arr" "a", "array_prop_idx" 0}
                 {"array_prop_arr" "b", "array_prop_idx" 1}))))
  (testing "Nested arrays should get denormalized"
    (is (= (denormalize {"nested_array_prop" [["a"] ["b"]]})
           (list {"nested_array_prop_idx" 0,
                  "nested_array_prop_arr_arr" "a",
                  "nested_array_prop_arr_idx" 0}
                 {"nested_array_prop_idx" 1,
                  "nested_array_prop_arr_arr" "b",
                  "nested_array_prop_arr_idx" 0}))))
  (testing "Maps should get flattened"
    (is (= (denormalize {"map_prop" {"key" "val"}})
           (list {"map_prop_dot_key" "val"}))))
  (testing "Nested maps should get flattened"
    (is (= (denormalize {"nestedmap_prop" {"sub_map" {"sub_key" "val"}}})
           (list {"nestedmap_prop_dot_sub_map_dot_sub_key" "val"}))))
  (testing "Nested arrays and nested maps get handled together ok"
    (is (= (denormalize {"nestedmap_prop" {"sub_map" {"sub_key" "val"}}
                         "nested_array_prop" [["a"] ["b"]]})
           (list {"nestedmap_prop_dot_sub_map_dot_sub_key" "val",
                  "nested_array_prop_idx" 0,
                  "nested_array_prop_arr_arr" "a",
                  "nested_array_prop_arr_idx" 0}
                 {"nestedmap_prop_dot_sub_map_dot_sub_key" "val",
                  "nested_array_prop_idx" 1,
                  "nested_array_prop_arr_arr" "b",
                  "nested_array_prop_arr_idx" 0})))))
