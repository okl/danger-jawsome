(ns jawsome-core.xform.xforms.hoist-test
  "Implements xform pipeline step: Hoist tests"
  {:author "Matt Halverson"
   :date "2/13/2014"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.hoist :refer [hoist]])
  (:require [roxxi.utils.print :refer [print-expr]]))

(def test-map {:a {:b 2 :c 3},
               :f {:g 7 :h 8},
               :m {:n 14 :o 15},
               :w {:x 24 :y 25}
               :top {:nest {:nestnest {:foo "bar"}}}})

(def hoist-cfg [{:properties [:a [:top :nest :nestnest] :f]
                 :type "hoist-once-for-property"
                 :prefix "pre_"}
                {:properties [:m]
                 :type "hoist-once-for-property"
                 :prefix "pre!"
                 :suffix "!post"}
                {:properties [:w]
                 :type "hoist-once-for-property"}])

(deftest hoist-test
  (testing "Testing hoisting for"
    (testing "empty map"
      (is (= (hoist {} hoist-cfg)
             {})))
    (testing "empty config"
      (is (= (hoist test-map [])
             test-map)))
    (testing "everything at once"
      (is (= (hoist test-map hoist-cfg)
             {"pre_:b" 2,
              "pre_:c" 3,
              :y 25,
              :x 24,
              "pre_:foo" "bar",
              "pre_:g" 7,
              "pre_:h" 8,
              "pre!:o!post" 15,
              "pre!:n!post" 14})))))
