(ns jawsome-core.reader.json.core
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [cheshire.core :refer [parse-string]])
  (:require [jawsome-core.reader.json.xform])
  (:require [jawsome-core.reader.json.xforms.unicode :refer [unicode-recode]])
  (:require [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]]))

;; Just rebind the parsing to cheshire directly.
;; No need to improve upon something that's great.


(defn make-json-reader
  "Returns a function that takes a string and returns a
clojure map representing analogous to the JSON string"
  [& {:keys [pre-xform remove-cruft? unicode-recode? key-fn array-coerce-fn]
      :or {remove-cruft? true
           recode-unicode? true}}]
  (let [parser (fn cheshire-parser [s]
                 (parse-string s key-fn array-coerce-fn))
        pre-xforms (filter identity
                           (list (and remove-cruft? remove-cruft)
                                 (and unicode-recode? unicode-recode)
                                 (and pre-xform)))
        pre-read-xform (apply comp pre-xforms)]
    (fn json-reader [string]
      (parser (pre-read-xform string)))))
