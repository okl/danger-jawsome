(ns jawsome-dsl.core
  "Implementing a mini-language for jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [diesel.core :refer [definterpreter]]
            [jawsome-core.reader.json.core :as r]
            [jawsome-core.xform.core :as x]
            [roxxi.utils.print :refer [print-expr]]))

(def j "{\"referer\": \"http://a.tellapart.com/ai?d=160x600&nid=b43399ef-0e07-40bf-be61-dda86b726b17&pn=&dcu=aHR0cDovL29uZWtpbmdzbGFuZS5jb20=&n=Gh2CZpEnxTmu&openxid=8567a8e6-3059-4bc9-8e64-ef1665b3716a&as=rt&bm=MQvTZrFduqGP&oms=ABJeb19Ri1q2CRTAjdgBQsr2Gt4ViNLfeCEKnI7yEfXejcLFguDACZ-3DrvWr9V-Rh7uOumofkaV&openxp=AAABPyVaCuBAF8VMzQWxR4j5foifEX3ja7OdWw&cu=http://ox-d.monetizationservices.servedbyopenx.com/w/1.0/rc?ts=1fHJhaWQ9ODZxMzcwNzIxMjI1fG1tZj00Mzc1fHBpPTUwOTZ8bXJjPVNSVF9XT058cHI9NTA5Ng&r=&uid=Uid(valid=True,%20encoded=u'ABJeb1_WRM36tja2lqd-KkibOHkdSECL48tO34UnNkbzLTW3-3_5nb6fz5RPDmxr97rjE4XPHDHoa0830fN6bT4xGXfIqO3f6g',%20decoded='\\xaaf\\xbd\\x10]\\xc1\\x1d\\xa0\\x111\\xbf\\x9b\\xe0\\xb0\\x0f\\x15')\"}")

(defn- valid-xform? [[keyword & options] registry]
  (contains? registry (symbol (name keyword))))

(defn- keyword-to-xform [[keyword & options] registry]
  (cons (get registry (symbol (name keyword)))
        options))

(defn- lookup-xforms-in-registry [good-pairs registry]
  (map #(keyword-to-xform % registry) good-pairs))

(defn- functionify-xforms [cfg registry]
  (let [partitioned (partition-by keyword? cfg)
        xform*enabled?*args (map #(apply concat %) (partition 2 partitioned))
        enabled-trios (filter second xform*enabled?*args)
        xform*args (map #(cons (first %) (drop 2 %)) enabled-trios)
        bad-pairs (remove #(valid-xform? % registry) xform*args)
        good-pairs (filter #(valid-xform? % registry) xform*args)
        bad-names (map first bad-pairs)
        looked-up-good-pairs (lookup-xforms-in-registry good-pairs registry)]
    [looked-up-good-pairs
     bad-names]))

(definterpreter read-interp []
  ['xforms => :xforms])
(defmethod read-interp :xforms [[_ & read-cfg]]
  (let [[good-pairs bad-names] (functionify-xforms read-cfg r/xform-registry)
        xforms (map first good-pairs)
        composed-xform (apply comp (list* list xforms))]
    (if (empty? bad-names)
      (let [json-reader (r/make-json-reader :pre-xform composed-xform)]
        (println "j parses to" (r/read-str json-reader j)))
      (println "unrecognized transforms: " bad-names)
      )))

(definterpreter xform-interp []
  ['xforms => :xforms])
(defmethod xform-interp :xforms [[_ & xform-cfg]]
  (let [[good-pairs bad-names] (functionify-xforms xform-cfg x/xform-registry)
        ;; composed-xform (apply comp xforms)
        ]
    (print-expr good-pairs)
    (print-expr bad-names)))


(definterpreter pipeline-interp []
  ['pipeline => :pipeline]
  ['read => :read]
  ['xform => :xform]
  ['convert-format => :convert-format] ;gather-schema-and-convert-to-csv-or-other-form phase
)

(defmethod pipeline-interp :pipeline [[_ & steps]]
  (doall (map pipeline-interp steps)))

(defmethod pipeline-interp :read [[_ xforms]]
  (do
    (println "Read cfg is" xforms)
    (read-interp xforms)))
(defmethod pipeline-interp :xform [[_ xforms]]
  (do
    (println "Xform cfg is" xforms)
    (xform-interp xforms)))
(defmethod pipeline-interp :convert-format [[_ & convert-cfg]]
  (do
    (println "Convert-format cfg is" convert-cfg)))


(pipeline-interp
 '(pipeline
   (read (xforms
          :unicode-recode true
          :remove-cruft true
          ))
   (xform (xforms
           :reify-values true foo
           :make-property-remapper false
           :make-value-type-filter false
           :make-value-synonymizer false
           :static-value-merge-fn false
           :default-value-merge-fn true
           :prune-nils true bar baz
           :denormalize-map false))
   ))
