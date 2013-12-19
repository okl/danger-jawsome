(ns jawsome-core.reader.json.xform
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [jawsome-core.reader.json.xforms.unicode :refer [unicode-recode]])
  (:require [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]]))


(defprotocol Transform
  (xform [_ string] "Takes a string to be interpreted as json
and returns a seq of strings to be interpreted as json"))


(defn- seqify
  "Some functions may just take a string and return a string,
this helps us ensure that we don't have to wrap functions
to meet the criteria outlined above"
  [s]
  (if (string? s)
    (list s)
    (seq s)))

(defmacro wrap-simple-xform
  "Takes a function that maintains the requirement
of the protocol above, and creates a Transform factory function"
  [f]
  (let [name (if (.startsWith (str f) "make-")
               (str f "-xform")
               (str "make-" f "-xform"))]
    `(defn ~(symbol name) []
       (reify Transform
         (xform [_ ~'s]
           (seqify (~f ~'s)))))))

(wrap-simple-xform remove-cruft) ;; make-remove-cruft-xform
(wrap-simple-xform unicode-recode) ;; make-unicode-recode-xform
