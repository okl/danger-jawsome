(ns jawsome-dsl.utils
  "Utils for jawsome-dsl"
  {:author "Matt Halverson"
   :date "2014/03/31"}
  (:require [roxxi.utils.print :refer [print-expr]]))

(defmacro log-and-throw [error-msg]
  `(do
     (clojure.tools.logging/error ~error-msg)
     (throw (RuntimeException. ~error-msg))))

(defmacro log-and-return [prefix-string thing]
  `(do
     (let [pretty-thing# (with-out-str (clojure.pprint/pprint ~thing))
           cleaner-thing# (clojure.string/trim pretty-thing#)]
       (clojure.tools.logging/info (str ~prefix-string "\n" cleaner-thing#))
       ~thing)))
