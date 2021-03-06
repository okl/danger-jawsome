(ns jawsome-core.reader.json.xforms.cruft
  {:author "Alex Bahouth"
   :date "11/14/2013"}
  (:require [clojure.tools.logging :as log])
  (:require [roxxi.utils.common :refer [def-]]))

;; # Criteria for lines we even want to try to read from a file.

;; ## Remove any text preceding JSON
;; If the lines of the file have data that looks like
;; `apache-store-json_ssl.log.2:{"a":"b", ...}`
;; get rid of the leading markup so they come out as
;; `{"a":"b", ...}`
;;
(defn- line-starts-with-garbage?
  "Returns true if the line doesn't start with a '{'"
  [line]
  (not (= (get line 0) \{)))

(defn- line-ends-with-garbage?
  "Returns true if the line doesn't start with a '{'"
  [line]
  (let [end-pos (- (count line) 1)]
    (not (= (get line end-pos) \}))))

(def- msg
"This line seems to be garbage, doesn't start with '{', or end with '}':")

(defn- remove-extraneous-line-markup [line]
  (if (or (line-starts-with-garbage? line)
          (line-ends-with-garbage? line))
    (let [the-line-less-garbage (re-find #"\{.*\}" line)]
       (or the-line-less-garbage
           (do
             (log/warn (str msg line))
             "")))
    line))


;; Should handle comments and skipping lines with line comments
;; `    // {"foo": "bar"}`
;; would be thrown away. No matter how many leading spaces.
(defn- remove-single-line-comments [line]
  (if (re-find #"^\s*\/\/" line)
    ""
    line))


(defmacro empty-string-trap [str-fn->str]
  `(fn skip-if-empty-str [s#]
    (if (empty? s#)
       s#
       (~str-fn->str s#))))

;; See http://www.regular-expressions.info/unicode.html
(defn string-contains-control-character? [some-string]
  (re-find #"\p{C}" some-string))

(def no-control-characters?
  (complement string-contains-control-character?))

;; Top level predicate about whether or not we should
;; even consider trying to reason about a line
;;
(defn- acceptable-line? [log-line]
  (and (string? log-line)
       (no-control-characters? log-line)))

(defn- remove-unacceptable-line [line]
  (if (acceptable-line? line)
    line
    ""))


(def- remove-cruft-fn
  ;; Order matters! We should remove comments first!
  (comp (empty-string-trap remove-extraneous-line-markup)
        remove-single-line-comments
        remove-unacceptable-line))

(defn remove-cruft [s]
  (let [result (remove-cruft-fn s)]
    (if (empty? result) nil result)))
