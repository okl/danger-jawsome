(ns jawsome-dsl.init-registry
  "Requiring this module will initialize the registry for Jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/25"}
  (:require [jawsome-dsl.xform :refer [defxform defvar]])
  (:require [jawsome-core.reader.json.xforms.unicode :refer [unicode-recode]]
            [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]]
            [jawsome-core.reader.json.core :refer [make-json-reader-fn]])
  (:require [jawsome-core.xform.xforms.denormalize :refer [make-denormalize]]
            [jawsome-core.xform.xforms.hoist :refer [make-hoist]]
            [jawsome-core.xform.xforms.property-mapping :refer [make-property-remapper]]
            [jawsome-core.xform.xforms.pruning :refer [prune-nils]]
            [jawsome-core.xform.xforms.reify-values :refer [reify-values]]
            [jawsome-core.xform.xforms.static-injection :refer [static-value-merge-fn
                                                                default-value-merge-fn]]
            [jawsome-core.xform.xforms.synonyms :refer [make-value-synonymizer
                                                        make-path-specific-synonymizer]]
            [jawsome-core.xform.xforms.value-type-filter :refer [make-value-type-filter]]))

;; Read phase, ordered xforms
(defxform 'remove-cruft (constantly remove-cruft))
(defxform 'recode-unicode (constantly unicode-recode))
(defxform 'read-json make-json-reader-fn)

(def read-phase-ordering
  ['remove-cruft
   'recode-unicode
   'read-json])

;; Xform phase, ordered xforms
(defxform 'hoist make-hoist)
(defxform 'remap-properties make-property-remapper)
(defxform 'reify (constantly reify-values))
(defxform 'translate make-value-synonymizer)
(defxform 'translate-paths make-path-specific-synonymizer)
(defxform 'type-enforce make-value-type-filter)
(defxform 'denorm make-denormalize)

(def xform-phase-ordering
  ['hoist
   'remap-properties
   'reify
   'translate
   'translate-paths
   'type-enforce
   'denorm])

;; Xform phase, un-ordered xforms
(defxform 'static-values static-value-merge-fn)
(defxform 'default-values default-value-merge-fn)
(defxform 'prune-nils (constantly prune-nils))
;; TODO implement these:
;; - remove aka prune-paths
;; - only
;; - drop-if-particular-kv-occurs (e.g. path='/server-status?auto')
;; - drop-if-had-to-type-enforce
;;it is worth remarking that the 'default ordered xforms'
;; can also be treated as library, of course.
