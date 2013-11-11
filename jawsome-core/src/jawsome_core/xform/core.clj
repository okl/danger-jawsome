(ns jawsome-core.xform.core
  {:author "Alex Bahouth"
   :date "11/10/2013"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:use jawsome-core.xform.xforms.property-mapping
        jawsome-core.xform.xforms.pruning
        jawsome-core.xform.xforms.reify-values
        jawsome-core.xform.xforms.static-injection
        jawsome-core.xform.xforms.synonyms
        jawsome-core.xform.xforms.value-type-filter))


;; In order to provide a hook to users to define their own
;; transforms, all they need do is implement this protocol.
(defprotocol Transform
  (xform [_ m] "Takes a map and returns a map, nil, or false"))

;; In fact, we'll do the same for our OOB transforms.

(defmacro wrap-simple-xform
  "Takes a function that maintains the requirement
of the protocol above, and creates a Transform factory function"
  [f]
  `(defn ~(symbol (str "make-" f "-xform")) []
     (reify Transform
       (xform [_ ~'m] (~f ~'m)))))

(defmacro wrap-init-xform
  "Takes a function that maintains the requirement
of the protocol above, and creates a Transform factory function,
passing any intialization values required directly to the
function constructors"
  [f]
  `(defn ~(symbol (str f "-xform")) [& ~'init-vals]
     (let [~'initd-fn (apply ~f ~'init-vals)]
       (reify Transform
         (xform [_ ~'m] (~'initd-fn ~'m))))))

;; make-prune-nils-xform, make-reify-values-xform, etc.
(wrap-simple-xform prune-nils)
(wrap-simple-xform reify-values)

;; make-property-remapper-xform, static-value-merge-fn-xform, etc.
(wrap-init-xform make-property-remapper)
(wrap-init-xform make-value-type-filter)
(wrap-init-xform make-value-synonymizer)
(wrap-init-xform static-value-merge-fn)
(wrap-init-xform default-value-merge-fn)
