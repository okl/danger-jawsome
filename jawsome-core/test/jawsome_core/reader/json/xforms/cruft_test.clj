(ns jawsome-core.reader.json.xforms.cruft-test
  (:require [clojure.test :refer :all]
            [roxxi.utils.print :refer :all])
  (:require [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]]))


(defn- resource [filename]
  (str "test/jawsome_core/resources/reader/json/xforms/cruft/" filename))


(deftest remove-extraenous-line-markup-test
  "Testing if we can parse JSON lines out of non-comment blocks of text"
  (let [[clean start end start-n-end no-front-brace no-back-brace
         no-braces]
        (clojure.string/split
         (slurp (resource "extraneous-line-markup.json"))
         #"\n")]
    (testing "with a clean line"
      (is (= (remove-cruft clean) clean)))
    (testing "with a line with leading cruft"
      (is (= (remove-cruft start) clean)))
    (testing "with a line ending with cruft"
      (is (= (remove-cruft end) clean)))
    (testing "with a line caked in cruft"
      (is (= (remove-cruft start-n-end) clean)))
    (testing "with a line missing a starting brace"
      (is (= (remove-cruft no-front-brace) "")))
    (testing "with a line missing an ending"
      (is (= (remove-cruft no-back-brace) "")))
    (testing "with a line that has no braces"
      (is (= (remove-cruft no-braces) "")))))


(deftest remove-null-data-line-test
  "Testing if we can handle rows of corrupted or bad data that has 
occured in the wild"
  (let [null-filled-data
        (clojure.string/split
         (slurp (resource "null-chars.json"))
         #"\n")]
     (testing "against a bunch of bad data!"
       (is (every? empty? (map remove-cruft null-filled-data))))))

(deftest remove-single-line-comments-test
  "Testing that we can correctly remove single line comments"
  (let [[clean start leading-space comments-only end-of-line]
        (clojure.string/split
         (slurp (resource "comments.json"))
         #"\n")]
    (testing "with a clean line"
      (is (= (remove-cruft clean) clean)))
    (testing "with comments at the start of the line"
      (is (= (remove-cruft start) "")))
    (testing "with whitespace in front of the first comments"
      (is (= (remove-cruft leading-space) "")))
    (testing "with no json, but comments"
      (is (= (remove-cruft comments-only) "")))
        (testing "comments after the JSON"
      (is (= (remove-cruft end-of-line) clean)))))

