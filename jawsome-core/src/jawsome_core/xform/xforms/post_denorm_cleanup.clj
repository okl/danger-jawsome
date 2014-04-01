(ns jawsome-core.xform.xforms.post-denorm-cleanup
  "Generic xforms that you'll probably want to apply post-denorm"
  {:author "Matt Halverson"
   :date "2014/03/31"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.collections :refer [project-map
                                             filter-map]]))

;; # sanitize-field-names

(defn- remove-naughty-chars [field-name]
  (clojure.string/replace field-name #"[^\w\d_]" "_"))

(defn sanitize-field-names [json-map]
  "Replace characters that aren't allowed to be in a column name,
and convert all the names to lower-case"
  (let [sanitize (comp remove-naughty-chars
                       clojure.string/lower-case)]
    (project-map json-map :key-xform sanitize)))

(defn make-sanitize-field-names []
  sanitize-field-names)

;; # remove-empty-string-fields

(defn- empty-string? [thing]
  (= thing ""))

(defn remove-empty-string-fields [json-map]
  (filter-map #(not (empty-string? (val %))) json-map))

(defn make-remove-empty-string-fields []
  remove-empty-string-fields)
