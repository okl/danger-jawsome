(ns jawsome-core.xform.xforms.reify-values
  "Implements xform pipeline step: String Value Reification"
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [cheshire.core :refer [parse-string]])
  (:require [roxxi.utils.collections :refer [project-map]]))

(defn- first-and-last-char-are [str-val first-c last-c]
  (and
   (= (get str-val 0) first-c)
   (= (get str-val (- (count str-val) 1)) last-c)))

(defn- array-ish? [str-val]
  (first-and-last-char-are str-val \[ \]))

(defn- map-ish? [str-val]
  (first-and-last-char-are str-val \{ \}))

(defn- unescape-one-level [string]
  ;; Previous
  ;; The idea here is simple in essence, but incredibly damn annoying...
  ;; Each level of escaping has an extra \. So we want to remove one \ for every
  ;; set of \'s we find.
  (clojure.string/replace string #"(\\+)\"" #(str (apply str (drop-last 1 (%1 1))) "\"")))

(defn- parsed-if-parsed [val]
  (when (not (string? val))
    val))

;; max 64 bit signed number:
;; 9,223,372,036,854,775,807 ~ 19 digits
;; Clojure can support bigger integers than this
;; but, let's assume that this might be used
;; by non-clojure code, and that there's a 64 bit max.
(defn- number-if-number [val]
  (and (string? val)
       (or (re-matches #"(^[\-]?[1-9]\d*$)|(^[\-]?0$)" val)
           (re-matches #"(^[\-]?[1-9]\d*\.\d+$)|(^[\-]?0\.\d+$)" val))
       (let [attempt (read-string val)]
         (and (number? attempt)
              (or (instance? Long attempt)
                  (instance? Double attempt))
              attempt))))

(defn possibly-inner-escaped-data? [string]
  ;; This seems like a strange condition...
  ;; but what we're saying is "if there are 2 or more backslashes
  ;; in a row in this string, we probably need to peel
  ;; things back one level before we try to parse the string
  ;; otherwise, we can just try parsing it.
  ;;
  ;; If there is only one backslash, it's probably escaping
  ;; some quotes that are inside- but if there is more than one,
  ;; that means we probably have additional data inside
  ;; that's further escaped.
  (or (re-find #"\\{2,}" string)
      (re-find #"\\+\"" string)))

(declare reify-value)

(defn- array-if-array [val]
  (when (and (string? val) (array-ish? val))
    (if (possibly-inner-escaped-data? val)
      (try
        (vec (map reify-value (parse-string (unescape-one-level val))))
        (catch com.fasterxml.jackson.core.JsonParseException e
          (try
            (vec (map reify-value (parse-string val)))
            (catch com.fasterxml.jackson.core.JsonParseException e
              ;; (log-warn here) maybe? optionally?
              nil))))
      (try
        (vec (map reify-value (parse-string val)))
        (catch com.fasterxml.jackson.core.JsonParseException e
          ;; (log-warn here) maybe? optionally?
          nil)))))

(defn- map-if-map [val]
  (when (and (string? val) (map-ish? val))
    (if (possibly-inner-escaped-data? val)
      (try
        (let [base-json (parse-string (unescape-one-level val))]
          (project-map base-json :value-xform reify-value))
          (catch com.fasterxml.jackson.core.JsonParseException e
            (try
              (let [base-json (parse-string val)]
                (project-map base-json :value-xform reify-value))
              (catch com.fasterxml.jackson.core.JsonParseException e
                ;; (log-warn here) maybe? optionally?
                nil))))
      (try
        (let [base-json (parse-string val)]
          (project-map base-json :value-xform reify-value))
        (catch com.fasterxml.jackson.core.JsonParseException e
          ;; (log-warn here) maybe? optionally?
          nil)))))

(defn- reify-value [val]
  (or (map-if-map val)
      (array-if-array val)
      (number-if-number val)
      (parsed-if-parsed val)
      val)) ;; else string

(defn reify-values
  "Takes a map, and examines each value looking for strings- if a string is found
is it analyzed to determine whether or not it contains JSON content that can be
read into a clojure object: either a map, array, number, or the identity case of an
already parsed value.

When a map or an array is found, this function is recursively applied to each of
its members, to continue parsing out values.

Additionally, since nested maps and vectors can often have their keys and values escaped
additional quotes when nesting, this function is able to parse escaped JSON to unbox
inner escaped JSON and return one materialized view of the map that is no longer
un-escapeable. There is no limit to the amount of escaped JSON that can be handled.

Note, this does not handle the reification of nulls- i.e. \"null\" or \"undefined\".
But, if they are nested inside of escaped JSON they will be elevated to top level strings
represented by \"null\"  as opposed to \"\\\\\\null\"\\\\\\\"
See `value-synonym-mapping` for assistance with null reification"
  [some-map]
  (project-map some-map :value-xform reify-value))
