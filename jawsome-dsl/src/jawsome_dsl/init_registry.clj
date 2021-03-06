(ns jawsome-dsl.init-registry
  "Running the `init` function in this module will init the xform-registry"
  {:author "Matt Halverson"
   :date "2014/02/25"}
  (:require [jawsome-dsl.xform :refer [defxform defvar]])
  (:require [jawsome-core.reader.json.xforms.unicode :refer [unicode-recode]]
            [jawsome-core.reader.json.xforms.cruft :refer [remove-cruft]]
            [jawsome-core.reader.json.core :refer [make-json-reader-fn]])
  (:require [jawsome-core.xform.xforms.denormalize :refer [make-denormalize]]
            [jawsome-core.xform.xforms.hoist :refer [make-hoist]]
            [jawsome-core.xform.xforms.log :refer [make-log]]
            [jawsome-core.xform.xforms.post-denorm-cleanup
             :refer [make-sanitize-field-names
                     make-remove-empty-string-fields]]
            [jawsome-core.xform.xforms.property-mapping
             :refer [make-property-remapper]]
            [jawsome-core.xform.xforms.pruning :refer [make-prune-nils
                                                       make-prune-paths
                                                       make-keep-paths]]
            [jawsome-core.xform.xforms.reify-values :refer [make-reify-values]]
            [jawsome-core.xform.xforms.static-injection
             :refer [static-value-merge-fn
                     default-value-merge-fn]]
            [jawsome-core.xform.xforms.synonyms
             :refer [make-value-synonymizer
                     make-path-specific-synonymizer]]
            [jawsome-core.xform.xforms.value-type-filter
             :refer [make-value-type-filter]]))

(defn init
  "Loads the fxns below into the registry"
  []

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
  (defxform 'reify make-reify-values)
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

  ;; Xform phase, xforms which are ENABLED BY DEFAULT and will
  ;; occur after the rest of the xform-phase xforms, unless you
  ;; pass in an environment that disables them such as
  ;; `jawsome-dsl.denorm/env-to-disable-post-denorm-cleanup`
  (defxform 'sanitize-field-names make-sanitize-field-names)
  (defxform 'remove-empty-strings make-remove-empty-string-fields)

  ;; Xform phase, un-ordered xforms
  (defxform 'static-values static-value-merge-fn)
  (defxform 'default-values default-value-merge-fn)
  (defxform 'log make-log)
  (defxform 'prune-nils make-prune-nils)
  (defxform 'prune-paths make-prune-paths)
  (defxform 'remove make-prune-paths)
  (defxform 'keep-paths make-keep-paths)
  (defxform 'only make-keep-paths)

  ;; TODO implement these:
  ;; - drop-if-particular-kv-occurs (e.g. path='/server-status?auto')
  ;; - drop-if-had-to-type-enforce
  )
