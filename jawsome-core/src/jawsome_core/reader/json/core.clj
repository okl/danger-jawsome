(ns jawsome-core.reader.json.core
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [cheshire.core :refer [parse-string]])
  (:require [jawsome-core.reader.json.xform]))

;; Just rebind these to cheshire directly. No need to improve upon something that's great.


(defprotocol JsonReader
  (read-str [_ string]
    "Returns a clojure map representing a json string"))

(defn make-json-reader [& {:keys [pre-xform key-fn array-coerce-fn]}]
  (let [parser (fn cheshire-parser [s]
                 (parse-string s key-fn array-coerce-fn))]
    (if pre-read-xform
      (reify JsonReader
        (read-str [_ string]
          (map parser (pre-xform string))))
      (reify JsonReader [_ string]
        (parser string)))))
