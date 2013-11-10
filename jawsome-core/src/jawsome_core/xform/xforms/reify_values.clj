(ns jawsome-core.xform.xforms.reify-values
  "Implements xform pipeline step: String Value Reification

Largely comes as a readaptation from roxxi/jsonschema parser.clj"
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

(defn- parsed-if-parsed [v]
  (when (not (string? v))
    v))

(defn- special-value? 
  "returns true if this is a special JSON value of true, false, or null"
  [v]
  (#{"true" "false" "null"} v))


;; max 64 bit signed number:
;; 9,223,372,036,854,775,807 ~ 19 digits
;; Clojure can support bigger integers than this
;; but, let's assume that this might be used
;; by non-clojure code, and that there's a 64 bit max.
(defn- number-if-number [v]
  (and (string? v)
       (or (re-matches #"(^[\-]?[1-9]\d*$)|(^[\-]?0$)" v)
           (re-matches #"(^[\-]?[1-9]\d*\.\d+$)|(^[\-]?0\.\d+$)" v))
       (let [attempt (read-string v)]
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


(defn- parse-if-parsible [string]
  (try 
    (parse-string string)
    (catch com.fasterxml.jackson.core.JsonParseException e
      false)))

(defn- array-if-array [v]
  (when (and (string? v) (array-ish? v))
    (if (possibly-inner-escaped-data? v)
      (loop [escaped v]
        (let [unescaped (unescape-one-level escaped)
              parsed (parse-if-parsible unescaped)]
          (cond
           parsed (vec (map reify-value v))
           (= escaped unescaped)
           ;; log-warn here?
           nil
           :else (recur unescaped))))
      (try
        (vec (map reify-value (parse-string v)))
        (catch com.fasterxml.jackson.core.JsonParseException e
          ;; (log-warn here) maybe? optionally?
          nil)))))

(defn- map-if-map [v]
  (when (and (string? v) (map-ish? v))
    (if (possibly-inner-escaped-data? v)
      (loop [escaped v]
        (let [unescaped (unescape-one-level escaped)
              parsed (parse-if-parsible unescaped)]
          (cond
           parsed (project-map parsed :value-xform reify-value)
           (= escaped unescaped)
           ;; log-warn here?
           nil
           :else (recur unescaped))))
      (try
        (let [base-json (parse-string v)]
          (project-map base-json :value-xform reify-value))
        (catch com.fasterxml.jackson.core.JsonParseException e
          ;; (log-warn here) maybe? optionally?
          nil)))))

(defn- reify-value [v]
  ;; JSON `true`, `false`, and `null` require special consideration
  (if (special-value? v)
    (parse-string v)
    (or (map-if-map v)
        (array-if-array v)
        (number-if-number v)
        (parsed-if-parsed v)
        v))) ;; else string

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
un-escapeable. There is no limit to the amount of escaped JSON that can be handled."
  [m]
  (project-map m :value-xform reify-value))
