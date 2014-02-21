(ns jawsome-core.xform.xforms.hoist
  "Implements xform pipeline step: Hoist"
  {:author "Matt Halverson"
   :date "2/13/2014"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [roxxi.utils.collections :refer [dissoc-in
                                             project-map]]))

(defn- do-a-hoist [json-map property-path & {:keys [key-renamer]
                                             :or {key-renamer identity}}]
  (let [to-be-hoisted (get-in json-map property-path)
        renamed-sub-props (project-map to-be-hoisted :key-xform key-renamer)]
    (merge (dissoc-in json-map property-path)
           renamed-sub-props)))

(defn- make-rename-fn [prefix suffix]
  (if (and (nil? prefix)
           (nil? suffix))
    identity
    #(str prefix % suffix)))

(defn- pathify-props [property-paths]
  (map #(if (and (not (vector? %))
                 (not (list? %)))
          (vector %)
          %)
       property-paths))

(defn- prepare-hoist-once-for-property [hoist-cfg]
  (let [rename-fn (make-rename-fn (:prefix hoist-cfg) (:suffix hoist-cfg))
        props-to-rename (pathify-props (:properties hoist-cfg))]
    (if (empty? props-to-rename)
      identity
      (fn [m]
        (reduce #(do-a-hoist %1 %2 :key-renamer rename-fn)
                m
                props-to-rename)))))

(defn- prepare-one-hoist [hoist-cfg]
  (let [type (:type hoist-cfg)]
    (cond
     (=  type "hoist-once-for-property")
     (prepare-hoist-once-for-property hoist-cfg)
     :else
     (throw (RuntimeException.
             (str "Unknown key-joiner type " type))))))

(defn make-hoist [hoist-cfgs]
  (let [individual-hoists (map prepare-one-hoist hoist-cfgs)
        composed-hoist (apply comp individual-hoists)]
    composed-hoist))

(defn hoist [json-map hoist-cfgs]
  (let [hoist-fn (make-hoist hoist-cfgs)]
    (hoist-fn json-map)))
