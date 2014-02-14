(ns jawsome-core.xform.xforms.hoist
  "Implements xform pipeline step: Hoist"
  {:author "Matt Halverson"
   :date "2/13/2014"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [roxxi.utils.collections :refer [dissoc-in
                                             project-map]]))

(defn- do-a-hoist [json-map property-path & {:keys [key-renamer]
                                             :or {key-renamer identity}}]
  (let [property-path (if (and (not (vector? property-path))
                               (not (list? property-path)))
                        (vector property-path)
                        property-path)
        to-be-hoisted (get-in json-map property-path)
        renamed-sub-props (project-map to-be-hoisted :key-xform key-renamer)]
    (merge (dissoc-in json-map property-path)
           renamed-sub-props)))

(defn- make-rename-fn [prefix suffix]
  (if (and (nil? prefix)
           (nil? suffix))
    identity
    #(str prefix % suffix)))

(defn- hoist-once-for-property [json-map hoist-cfg]
  (let [rename-fn (make-rename-fn (:prefix hoist-cfg) (:suffix hoist-cfg))
        props-to-rename (:properties hoist-cfg)]
    (if (empty? props-to-rename)
      json-map
      (reduce #(do-a-hoist %1 %2 :key-renamer rename-fn)
              json-map
              props-to-rename))))

(defn- prepare-one-hoist [json-map hoist-cfg]
  (let [type (:type hoist-cfg)]
    (cond
     (=  type "hoist-once-for-property")
     (hoist-once-for-property json-map hoist-cfg)
     :else
     (throw (RuntimeException.
             (str "Unknown key-joiner type " type))))))

(defn hoist [json-map hoist-cfgs]
  (if (empty? hoist-cfgs)
    json-map
    (reduce #(prepare-one-hoist %1 %2)
            json-map
            hoist-cfgs)))
