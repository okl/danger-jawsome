(ns jawsome-cli.stuff
  "Support for core"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; l1-interp-with-schema-fold

;; (defprotocol XformAndSchemaProtocol
;;   (xform [_ m])
;;   (schema-so-far [_]))

;; (deftype XformAndSchema [actual-fxn cumulative-schema]
;;   XformAndSchemaProtocol
;;   (xform [_ m]
;;     (actual-fxn m))
;;   (schema-so-far [_]
;;     (get-cumulative-schema cumulative-schema)))

;; (defn l2->xform-and-schema
;;   ([l2]
;;      (l2->xform-and-schema l2 default-env))
;;   ([l2 env]
;;      (let [l1 (pipeline-interp l2 default-env)
;;            cumulative-schema (make-a-cumulative-schema)
;;            actual-fxn (l1-interp-with-schema-fold l1 (xform-registry) cumulative-schema)]
;;        (XformAndSchema. actual-fxn cumulative-schema))))

;; (defn l1-interp-with-schema-fold [l1-forms xform-registry cumulative-schema]
;;   (let [giant-composed-one-to-many-xform (l1-interp l1-forms xform-registry)]
;;     (fn [one-in]
;;       (let [many-out (giant-composed-one-to-many-xform one-in)
;;             many-out-types (map extract-type-simplifying many-out)
;;             for-side-effects (doall
;;                               (map #(mixin-schema! % cumulative-schema)
;;                                    many-out-types))]
;;         many-out))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; xform-across-a-seq

;; (def s1 (l2->xform-and-schema
;;          '(pipeline
;;            (xform-phase
;;             (xforms :reify :denorm)))))
;; (schema-so-far s1)
;; (xform s1 {"k1" "12341234", "k2" ["10000asdfjl", "20000asdf"]})
;; (schema-so-far s1)

;; (def s2 (l2->xform-and-schema
;;          '(pipeline
;;            (xform-phase
;;             (xforms :reify :denorm)))))
;; (schema-so-far s2)
;; (xform s2 {"k1" "1", "k2" ["100", "200"]})
;; (schema-so-far s2)
;; (xform s2 {"k1" "12", "k2" ["10", "200", "400"]})
;; (schema-so-far s2)

;; (defn xform-across-a-seq [l2-forms seq]
;;   (let [x-and-s (l2->xform-and-schema l2-forms)]
;;     (doseq [record seq]
;;       (doall
;;        (map println (xform x-and-s record))))
;;     (schema-so-far x-and-s)))

;; (xform-across-a-seq '(pipeline (xform-phase (xforms :reify :denorm)))
;;                     (list {"k1" "1", "k2" ["100", "200"]}
;;                           {"k1" "12", "k2" ["10", "200", "400"]}))
;; ;;#jsonschema.type_system.types.Document{:properties #{"k2_idx" "k1" "k2_arr"}, :map {"k1" #jsonschema.type_system.types.Int{:min 1, :max 12}, "k2_arr" #jsonschema.type_system.types.Str{:min 2, :max 3}, "k2_idx" #jsonschema.type_system.types.Int{:min 0, :max 2}}}

;; (defn xform-across-an-input-stream [l2-forms input-stream]
;;   (let [seq (line-seq (java.io.BufferedReader. input-stream))]
;;     (xform-across-a-seq l2-forms seq)))
