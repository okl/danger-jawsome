(ns jawsome-core.xform.xforms.hoist
  {:author "Matt Halverson"
   :date "11/10/2013"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [roxxi.utils.collections :refer [extract-map]])
  (:require [denormal.keys :as keys]
            [denormal.core :as den]))

;; (defn- hoist-once-for-property [cfg]
;;   (keys/make-hoist-joiner (or (:prefix cfg) "")
;;                           (or (:suffix cfg) "")))

;; (defn- key-joiner-type [cfg]
;;   (:type cfg))

;; (defn- key-joiner-config->key-joiner [cfg]
;;   (let [type (key-joiner-type cfg)]
;;     (cond
;;      (=  type "hoist-once-for-property")
;;      (hoist-once-for-property cfg)
;;      :else
;;      (throw (RuntimeException.
;;              (str "Unknown key-joiner type " type))))))



;; (defn- key-joiners-for-properties [cfg]
;;   (let [key-joiner (key-joiner-config->key-joiner cfg)]
;;     (extract-map (:properties cfg)
;;                  :value-extractor (fn [_] key-joiner))))

;; (defn- make-key-joiner-from-configurations [cfgs]
;;   (keys/make-outer-property-dispatch-key-joiner
;;    (apply merge (map key-joiners-for-properties cfgs))
;;    den/default-key-joiner))

;; (defn- prepare-hoist [hoist-cfgs]
;;   (let [joiner (make-key-joiner-from-configurations hoist-cfgs)]
;;     joiner
;; ))

;; (defn hoist [json-map hoist-cfgs]
;;   (let [hoist-fn (if (empty? hoist-cfgs)
;;                    identity
;;                    (prepare-hoist hoist-cfgs))]
;;     (hoist-fn json-map)))
