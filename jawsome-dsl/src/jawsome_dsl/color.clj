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

(def sku-color-xform-pt-3
  (pipeline-interp
   '(pipeline
     (xform-phase
      (xforms
       :denormalize-map true)))))


;;SHOULD WORK!!
(defn shawns-fn [my-map]
  (let [first-elem (first my-map)
        num-colors (count (get-in first-elem ["value",0]))]
    (print-expr first-elem)
    {"sku_id" (get first-elem "key")
     "color_id"
    (vec
     (map #(hash-map
           "color" (get-in first-elem ["value",0,%])
           "weight" (get-in first-elem ["value",1,%]))
         (range num-colors))
     )
    }
    ))

;;ORIG
;; (defn hack-the-middle [foo]
;;   (sku-color-xform-pt-2 (print-expr (shawns-fn (sku-color-xform foo)))))

;; HACKED
(defn hack-the-middle [foo]
   (sku-color-xform-pt-2
    ;;(shawns-fn2
     (print-expr (shawns-fn (sku-color-xform (print-expr foo))))))
   ;;)
  ;;)
;; call first in shawn's function


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
    (let [record-seq (hack-the-middle line)] ;; <~~ hack the middle
      (doall
       (map println
            (map generate-string
                  (apply concat record-seq)))))))
