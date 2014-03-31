(ns jawsome-dsl.separate-phases
  "The top-level pipeline interp need"
  {:author "Matt Halverson"
   :date "2014/02/12"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [clojure.pprint :refer [pprint]]))

;; # Assertions

(defn- assert-num-phases [coll phase min max]
  (when (or (< (count coll) min)
            (> (count coll) max))
    (let [num-expected (if (= min max)
                         (str "exactly " min)
                         (str "between " min " and " max))]
      (throw
       (RuntimeException.
        (str "Invalid config! Expected " num-expected " " phase
             " phases, but found " (count coll) " " phase " phases: "
             (with-out-str (pprint coll))))))))

(defn- assert-zero-or-one [coll phase]
  (assert-num-phases coll phase 0 1))
(defn- assert-one [coll phase]
  (assert-num-phases coll phase 1 1))

;; # Denorm-phase defns

(defn- read-phase?  [form] (= (first form) 'read-phase))
(defn- xform-phase? [form] (= (first form) 'xform-phase))

(defn separate-denorm-phases [phases]
  (let [reads  (filter read-phase?  phases)
        xforms (filter xform-phase? phases)]
    (assert-zero-or-one reads "read")
    (assert-one xforms "xform")
    [(first reads) (first xforms)]))

;; # Top-level defns

(defn- denorm-phase?  [form] (= (first form) 'denorm-phase))
(defn- schema-phase?  [form] (= (first form) 'schema-phase))
(defn- project-phase? [form] (= (first form) 'project-phase))

(defn separate-top-level-phases [phases]
  (let [denorms  (filter denorm-phase?  phases)
        schemas  (filter schema-phase?  phases)
        projects (filter project-phase? phases)]
    (assert-zero-or-one denorms "denorm")
    (assert-zero-or-one schemas "schema")
    (assert-zero-or-one projects "project")
    [(first denorms) (first schemas) (first projects)]))
