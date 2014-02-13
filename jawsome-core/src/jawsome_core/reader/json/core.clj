(ns jawsome-core.reader.json.core
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [cheshire.core :refer [parse-string]])
  (:require [jawsome-core.common-utils :refer [defregistry]])
  (:require [jawsome-core.reader.json.xforms.unicode :refer [unicode-recode]]
            [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]])
  (:require [roxxi.utils.print :refer [print-expr print-expr-hella-rec]]))

;; Just rebind the parsing to cheshire directly.
;; No need to improve upon something that's great.


(defn make-json-reader
  "Returns a function that takes a string and returns a
clojure map representing analogous to the JSON string"
  [& {:keys [pre-xform remove-cruft? unicode-recode? key-fn array-coerce-fn]
      :or {remove-cruft? true
           recode-unicode? true}}]
  (let [parser (fn cheshire-parser [s]
                 (parse-string s key-fn array-coerce-fn))]
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
