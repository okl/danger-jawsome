(ns jawsome-core.xform.core
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [roxxi.utils.collections :refer [reassoc-many]])
  (:require [jawsome-core.common-utils :refer [defregistry]])
  (:use jawsome-core.xform.xforms.denormalize
        jawsome-core.xform.xforms.hoist
        jawsome-core.xform.xforms.property-mapping
        jawsome-core.xform.xforms.pruning
        jawsome-core.xform.xforms.reify-values
        jawsome-core.xform.xforms.static-injection
        jawsome-core.xform.xforms.synonyms
        jawsome-core.xform.xforms.value-type-filter))


;; In order to provide a hook to users to define their own
;; transforms, all they need do is implement this protocol.
(defprotocol Transform
  (xform [_ m] "Takes a map and returns a seq of maps"))

;; Useful Macros for easily wrapping your own functions
;; to support the Transform protocol.

(defn- seqify [m-or-s]
  (if (map? m-or-s)
    (list m-or-s)
    (seq m-or-s)))

(defn- starts-with [x s]
  (.startsWith (str x) s))

(defn- make-factory-fn-name [fn-name]
  (if (starts-with fn-name "make-")
    (str fn-name "-xform")
    (str "make-" fn-name "-xform")))

(defmacro apply-if [x pred? conseq]
  `(let [x# ~x]
     (if (~pred? x#)
       (~conseq x#)
       x#)))

(defmacro wrap-simple-xform
  "Takes a function that maintains the requirement
of the protocol above, and creates a Transform factory function"
  [f]
  (let [name (make-factory-fn-name f)]
    `(defn ~(symbol name) []
       (reify Transform
         (xform [_ ~'m] (seqify (~f ~'m)))))))

(defmacro wrap-init-xform
  "Takes a function that maintains the requirement
of the protocol above, and creates a Transform factory function,
passing any intialization values required directly to the
function constructors"
  [f]
  (let [name (make-factory-fn-name f)]
    `(defn ~(symbol name) [& ~'init-vals]
       (let [~'initd-fn (apply ~f ~'init-vals)]
         (reify Transform
           (xform [_ ~'m] (seqify (~'initd-fn ~'m))))))))

;; (defmacro wrap-init-xforms
;;   [fns]
;;   (let [registered-names (map extract-name fns)]
;;   (loop [forms '(
;;   `(do ~@forms

;; In fact, let's wrap all of our "built-in" xforms.

;; make-prune-nils-xform, make-reify-values-xform, etc.
(wrap-simple-xform prune-nils)
(wrap-simple-xform reify-values)

;; make-property-remapper-xform, make-static-value-merge-fn-xform, etc.
(wrap-init-xform make-property-remapper)
(wrap-init-xform make-value-type-filter)
(wrap-init-xform make-value-synonymizer)
(wrap-init-xform static-value-merge-fn)
(wrap-init-xform default-value-merge-fn)
(wrap-init-xform denormalize-map)

(defn make-composite-xform
  "Create a Transform which applies each Transform in order from left to right"
  [xforms]
  (reify Transform
    (xform [_ m]
      (reduce (fn [ms x]
                (mapcat #(xform x %) ms))
              (list m)
              xforms))))

(defn make-composite-xform-from
  "Create a Transform which applies each Transform in order from left to right"
  [& xforms]
  (make-composite-xform xforms))

(defregistry xform-registry
  '(hoist
    property-remapper ;;one arg -- map of what to rename. see reassoc-many. it can take paths!
    ;;pre
    reify-values ;;no args
    global-synonymizer ;; one arg -- value=>synonym
    path-specific-synonymizer ;;
    value-type-filter ;; one arg -- path=>type
    ;;mid
    denormalize
    ;;post
    prune-nils ;;no args

    ;;things that are library, not ordered:
    ;; - prune-paths
    ;; - only
    ;; - remove
    ;; - drop-if-particular-kv-occurs (e.g. path='/server-status?auto')
    ;; - drop-if-had-to-type-enforce
    static-value-merge ;;one arg -- map of what to force-insert
    default-value-merge ;;one arg -- map of what to insert if not present
    ;;it is worth remarking that the 'default ordered xforms'
    ;; can also be treated as library, of course.
    ))
