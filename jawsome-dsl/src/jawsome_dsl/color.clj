(ns jawsome-dsl.color
  "Let's see if this thing is useful!"
  {:author "Alex Bahouth"
   :date "2014/02/13"}
  (:require [jawsome-dsl.core :refer [pipeline-interp]]
            [roxxi.utils.print :refer [print-expr]]
            [roxxi.utils.collections :refer [project-map]]
            [cheshire.core :refer [generate-string]]))


(def sku-color-db
  (pipeline-interp
   '(pipeline
     (read-phase (xforms
                  :remove-cruft true
                  :unicode-recode false))
     (xform-phase
      (xforms
       :reify-values true
       :denormalize-map true)))))

(def sku-color-xform
  (pipeline-interp
   '(pipeline
     (read-phase (xforms
                  :remove-cruft true
                  :unicode-recode false))
     (xform-phase
      (xforms
       :reify-values true)))))

(def sku-color-xform-pt-2
  (pipeline-interp
   '(pipeline
     (xform-phase
      (xforms
       :denormalize-map true)))))

(def hack-the-middle [foo]
  (sku-color-xform-pt-2 (shawns-fn (sku-color-xform foo))))





(defn map->xsv [some-map field-order delimiter]
  (clojure.string/join delimiter (map #(get some-map %) field-order)))


(def pipeline-id->pipeline-fn
  {"sku-color" sku-color-xform})

(defn sku-color-expansion [sku-color-map]
  (let [x (project-map sku-color-map  :value-xform (fn [x]
                                                     (if (.contains (str x) "_")
                                                       (clojure.string/split x #"_")
                                                       x)))

        y (assoc x  "key" (zipmap ["red" "green" "blue"] (get x "key")))
        z (assoc (get y "key") "pallet" (get y  "val"))]
    z))

;; {"sku_id" <sku-id>,
;;  "color_id" [color-ids+],
;;  "color_wt" [color-wt+],}

(defn -main []

  (doseq [line (line-seq (java.io.BufferedReader. *in*))]
    ;;The inner doall is because a single record of input produces
    ;; a (lazy) sequence of records of output.
    (let [record-seq (sku-color-xform line)] ;; <~~ hack the middle
      (doall
       (map println
            (map generate-string
                  (apply concat records-seq)))))))
