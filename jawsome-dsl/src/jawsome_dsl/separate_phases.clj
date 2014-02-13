(ns jawsome-dsl.separate-phases
  "The top-level pipeline interp need"
  {:author "Matt Halverson"
   :date "2014/02/12"}
  (:require [roxxi.utils.print :refer [print-expr]]
            [clojure.pprint :refer [pprint]]))

(defn- read-phase?    [form] (= (first form) 'read-phase))
(defn- xform-phase?   [form] (= (first form) 'xform-phase))
(defn- project-phase? [form] (= (first form) 'project-phase))

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

(defn separate-phases [phases]
  (let [reads    (filter read-phase?    phases)
        xforms   (filter xform-phase?   phases)
        projects (filter project-phase? phases)]
    (assert-zero-or-one reads "read")
    (assert-one xforms "xform")
    (assert-zero-or-one projects "project")
    [(first reads) (first xforms) (first projects)]))
