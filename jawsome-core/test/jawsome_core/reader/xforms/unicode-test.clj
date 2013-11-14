(ns jawsome-core.reader.xforms.unicode-test
  (:require [clojure.test :refer :all]
            [roxxi.utils.print :refer :all])
  (:require [jawsome-core.reader.xforms.utf :refer [unicode-recode]]))


(defn- resource [filename]
  (str "test/jawsome_core/resources/reader/xforms/unicode/" filename))


(deftest unicode-recode-test
  "Testing if we can parse JSON lines out of non-comment blocks of text"
  (let [[clean utf-8 uft-16]
        (clojure.string/split
         (slurp (resource "unicode.json"))
         #"\n")]
    (testing "with a clean line"
      (is (= (unicode-recode clean) clean)))
    (testing "recoding of utf-8"
      (is (= (unicode-recode utf-8) clean)))
    (testing "recoding of utf-16"
      (is (= (unicode-recode utf-16) clean)))))

