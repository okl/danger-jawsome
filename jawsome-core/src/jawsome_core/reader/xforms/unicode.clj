(ns jawsome-core.reader.xforms.unicode
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.print :refer [print-expr]]))


;; ### Handling UTF-8 Characters
;;
;; the \x's in the file are the hex representation
;; of characters. But each one does not necessarily
;; correspond to a single character.
;;
;; Experimentally we've seen that \xc3\xa9
;; translates to é if we interpret them as UTF-8
;;
;; If we were to translate them separately as hex values to, say, ASCII, 
;; we'd see that... well... they don't translate!
;; (highest hex-ASCII value is 7F)
;;
;; So how do we know for sure it's UTF-8? well...
;; good guess?

(defn- hex->byte [hex-str]
  (.byteValue ^Integer (Integer/parseInt hex-str 16)))

(defn- bytes->str [bytes]
  (String. ^bytes bytes "UTF-8"))

(defn unicode-recode [line]
  (clojure.string/replace line
                          #"((\\)+x[a-fA-F0-9]{2})+"
                          (fn [matches]
                            (let [hex-strs
                                  (rest (clojure.string/split (matches 0) #"(\\)+x"))]
                              (bytes->str (byte-array (map hex->byte hex-strs)))))))
