(ns jawsome-core.xform.xforms.value-type-filter-test
  "Implements xform pipeline step tests: Property path value-type filtering
 (i.e. type enforcement)"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [clojure.test :refer :all])
  (:require [roxxi.utils.collections :refer [project-map, filter-map]])
  (:require [jawsome-core.xform.xforms.value-type-filter
             :refer [make-value-type-filter]]))

(def path=>type
  {["number"] :number
   ["string"] :string
   ["true"] :boolean
   ["false"] :boolean
   ["array"] :array
   ["map"] :map
   ["map" "number"] :number
   ["map" "string"] :string
   ["map" "boolean"] :boolean
   ["map" "array"] :array
   ["map" "map"] :map
   })

(def vtf (make-value-type-filter path=>type))

(deftest value-type-filter-test
  (testing "Testing value-type-filtering"
    (testing "with flap maps of different types where"
      (testing"every key is asserted and matches"
      (let [m {"number" 5, "string" "hello", "true" true, "false" false,
               "array" [1 2 3], "map" {:a 5, :b 6}}]
        (is (= (vtf m) m))))
    (testing "every key is asserted and some don't match"
      (let [m {"number" 5, "string" "hello", "true" true, "false" false,
               "array" [1 2 3], "map" {:a 5, :b 6}}
            uh-oh-m (project-map m :value-xform str)]
        (is (= (vtf uh-oh-m)
               (filter-map (fn [[k v]] (string? v)) m)))))
    (testing "every not every key is asserted and some don't match"
      (let [m {"string" "hello", "other1" 10, "other2" {:a :c}
               "array" [1 2 3], "map" {:a 5, :b 6}}
            uh-oh-m (project-map m :value-xform str)]
        (is (= (vtf uh-oh-m)
               {"other1" "10", "other2" "{:a :c}", "string" "hello"})))))
    (testing "with nested maps of different types where"
      (testing"every key is asserted and matches"
        (let [nested {"number" 5, "string" "hello", "boolean" false,
                      "array" [1 2 3], "map" {:a 5, :b 6}}
              m {"number" 5, "string" "hello", "boolean" false,
                 "array" [1 2 3], "map" nested}]
          (is (= (vtf m) m))))
    (testing "every nested key is asserted and some don't match"
      (let [nested {"number" 5, "string" "hello", "boolean" false,
                    "array" [1 2 3], "map" {:a 5, :b 6}}
            uh-oh-nested (project-map nested :value-xform str)
            m {"number" 5, "string" "hello", "boolean" false,
               "array" [1 2 3], "map" uh-oh-nested}]
        (is (= (vtf m)
               (assoc m "map" (filter-map (fn [[k v]] (string? v)) m))))))
        (testing "some nested keys are asserted and some don't match"
      (let [nested {"number" 5, "string" "hello", "boolean" false,
                    "array" [1 2 3], "map" {:a 5, :b 6}}
            uh-oh-nested (project-map nested :value-xform str)
            other {"other1" 5 "other2" 10}
            uh-oh-nested+ (merge uh-oh-nested other)
            m {"number" 5, "string" "hello", "boolean" false,
               "array" [1 2 3], "map" uh-oh-nested+}]
        (is (= (vtf m)
               (assoc m "map" (merge (filter-map (fn [[k v]] (string? v)) m)
                                     other)))))))))

