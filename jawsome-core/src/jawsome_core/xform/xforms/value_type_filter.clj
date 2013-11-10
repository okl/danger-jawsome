(ns jawsome-core.xform.xforms.value-type-filter
  "Implements xform pipeline step: Property Renaming, Remapping and Pruning"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.collections :refer [dissoc-in project-map]]))

(def type=>satisfies-type?
  {:number number?
   :boolean #(or (= true %) (= false %))
   :string string?
   :array #(or (vector? %) (list? %) (set? %))
   :map map?})

(defn- remove-offending-fields
  "It's assumed that path and satisfies-type? are coming from a kv
  in a form like so:
    {[\"okl_params\" \"sales_event_id\"] :number
     [\"okl_params\" \"sku_id\"] :number}"
  [m [path satisfies-type?]]
  (if (satisfies-type? (get-in m path))
    m
    (dissoc-in m path)))

(defn make-value-type-filter [path=>type]
  (let [path=>satisfies-type?
        (project-map path=>type :value-xform type=>satisfies-type?)]
    (fn [m]
      (reduce remove-offending-fields m path=>satisfies-type?))))


;; {"okl_params": "{ \"sales_event_id\":\"12345a\", \"sku_id\":\"-9741*sfxd*139c91de-bd62-0601-d311-790a1dcb1fd5-0cb*sfxd*disableWs\" }"}
;; {"okl_params": "{ \"sales_event_id\":\"12345\", \"sku_id\":\"-9741*sfxd*139c91de-bd62-0601-d311-790a1dcb1fd5-0cb*sfxd*disableWsa\" }"}
;; {"okl_params": "{ \"sales_event_id\":\"12345\", \"sku_id\":\"9741\" }"}
;; {"bool_test_prop": true}
;; {"bool_test_prop": false}
;; {"bool_test_prop": 17 }
;; {"bool_test_prop": "17" }
;; {"bool_test_prop": "this is a string" }

;; # There are three acceptable types: number, string, and boolean
;; type-enforcement:
;;   - path: okl_params, sku_id
;;     type: number
;;   - path: okl_params, sales_event_id
;;     type: number
;;   - path: bool_test_prop
;;     type: boolean

;; (deftest does-type-enforcement-work-test
;;   (testing "Generalization of type enforcement! woot!"
;;     (let [[bad-sku-bad-sales-id
;;            bad-sku-good-sales-id
;;            good-sku-good-sales-id
;;            good-bool-true
;;            good-bool-false
;;            bad-bool-num
;;            bad-bool-num-as-str
;;            bad-bool-str]
;;           (clojure.string/split
;;            (slurp "test/resources/transform/type-enforcement-sample-data.json")
;;            #"\n")]
;;       (testing "bad sku, bad sales id"
;;         (is (= (transform bad-sku-bad-sales-id etl-config)
;;                (list {}))))
;;       (testing "bad sku, good sales id"
;;         (is (= (transform bad-sku-good-sales-id etl-config)
;;                (list {"sales_event_id" 12345}))))
;;       (testing "good sku, good sales id"
;;         (is (= (transform good-sku-good-sales-id etl-config)
;;                (list {"sales_event_id" 12345,
;;                       "sku_id", 9741}))))
;;       (testing "good bool, true"
;;         (is (= (transform good-bool-true etl-config)
;;                (list {"bool_test_prop" true}))))
;;       (testing "good bool, false"
;;         (is (= (transform good-bool-false etl-config)
;;                (list {"bool_test_prop" false}))))
;;       (testing "bad bool, number"
;;         (is (= (transform bad-bool-num etl-config)
;;                (list {}))))
;;       (testing "bad bool, number as string"
;;         (is (= (transform bad-bool-num-as-str etl-config)
;;                (list {}))))
;;       (testing "bad bool, string"
;;         (is (= (transform bad-bool-str etl-config)
;;                (list {})))))))
