(ns jawsome-core.reader.json.core
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [cheshire.core :refer [parse-string]])
  (:require [jawsome-core.common-utils :refer [defregistry]])
  (:require [jawsome-core.reader.json.xforms.unicode :refer [unicode-recode]]
            [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]])
  (:require [roxxi.utils.print :refer [print-expr
                                       print-expr-hella-rec]]))

;; Just rebind these to cheshire directly. No need to improve upon something that's great.


(defprotocol JsonReader
  (read-str [_ string]
    "Returns a clojure map representing a json string"))


(defn make-json-reader-fn [& {:keys [key-fn array-coerce-fn]}]
  (fn cheshire-parser [s]
    (parse-string s key-fn array-coerce-fn)))


(defn make-json-reader [& {:keys [pre-xform key-fn array-coerce-fn]}]
  (let [parser (make-json-reader-fn
                :key-fn key-fn
                :array-coerce-fn array-coerce-fn)]
    (if pre-xform
      (reify JsonReader
        (read-str [_ string]
          (map parser (pre-xform string))))
      (reify JsonReader
        (read-str [_ string]
          (parser string))))))


(defregistry xform-registry
  '(remove-cruft
    unicode-recode))
