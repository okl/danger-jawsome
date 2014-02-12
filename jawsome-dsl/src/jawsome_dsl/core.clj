(ns jawsome-dsl.core
  "Implementing a mini-language for jawsome pipelines"
  {:author "Matt Halverson"
   :date "2014/02/10"}
  (:require [diesel.core :refer [definterpreter]]
            [jawsome-core.reader.json.core :as r]
            [jawsome-core.xform.core :as x]
            [roxxi.utils.print :refer [print-expr]]))

(def j "{\"referer\": \"http://a.tellapart.com/ai?d=160x600&nid=b43399ef-0e07-40bf-be61-dda86b726b17&pn=&dcu=aHR0cDovL29uZWtpbmdzbGFuZS5jb20=&n=Gh2CZpEnxTmu&openxid=8567a8e6-3059-4bc9-8e64-ef1665b3716a&as=rt&bm=MQvTZrFduqGP&oms=ABJeb19Ri1q2CRTAjdgBQsr2Gt4ViNLfeCEKnI7yEfXejcLFguDACZ-3DrvWr9V-Rh7uOumofkaV&openxp=AAABPyVaCuBAF8VMzQWxR4j5foifEX3ja7OdWw&cu=http://ox-d.monetizationservices.servedbyopenx.com/w/1.0/rc?ts=1fHJhaWQ9ODZxMzcwNzIxMjI1fG1tZj00Mzc1fHBpPTUwOTZ8bXJjPVNSVF9XT058cHI9NTA5Ng&r=&uid=Uid(valid=True,%20encoded=u'ABJeb1_WRM36tja2lqd-KkibOHkdSECL48tO34UnNkbzLTW3-3_5nb6fz5RPDmxr97rjE4XPHDHoa0830fN6bT4xGXfIqO3f6g',%20decoded='\\xaaf\\xbd\\x10]\\xc1\\x1d\\xa0\\x111\\xbf\\x9b\\xe0\\xb0\\x0f\\x15')\"}")


(def ^:dynamic *registry* nil)
(defmacro with-registry [r & body]
  `(binding [*registry* ~r]
     ~@body))


(definterpreter pipeline-interp []
  ['pipeline => :pipeline]
  ['read-phase => :read-phase]
  ['xform-phase => :xform-phase]
  ['convert-format-phase => :convert-format-phase] ;gather-schema-and-convert-to-csv-or-other-form phase
  ['xforms => :xforms]
  ['xform => :xform]
)

(defmethod pipeline-interp :pipeline [[_ & steps]]
  (doall (map pipeline-interp steps)))

(defmethod pipeline-interp :read-phase [[_ xforms]]
  (do
    (println "Read cfg is" xforms)
    (with-registry r/xform-registry
      (pipeline-interp xforms))))
(defmethod pipeline-interp :xform-phase [[_ xforms]]
  (do
    (println "Xform cfg is" xforms)
    (with-registry x/xform-registry
      (pipeline-interp xforms))))
(defmethod pipeline-interp :convert-format-phase [[_ & convert-cfg]]
  (do
    (println "Convert-format cfg is" convert-cfg)))

(defmethod pipeline-interp :xforms [[_ & xforms]]
  (let [partitioned (partition-by keyword? xforms)
        xform*enabled?*args (map #(apply concat %) (partition 2 partitioned))
        prepend-xform (map #(list 'xform %) xform*enabled?*args)
        xforms (if (empty? prepend-xform)
                 [identity]
                 (cons list
                       (map read-interp prepend-xform)))
        composed-xform (apply comp xforms)
        json-reader (r/make-json-reader :pre-xform composed-xform)]
    (println "j parses to" (r/read-str json-reader j))))

(defn- enabled? [xform]
  (true? (second xform)))
(defn- keyword-to-xform [keyword registry]
  (get registry (symbol (name keyword))))
(defmethod pipeline-interp :xform [[_ xform]]
  (if (enabled? xform)
    (let [k (first xform)
          fxn (keyword-to-xform k *registry*)
          args (drop 2 xform)]
      #(apply fxn (conj args %)))
    (do
      (println "xform is disabled, skipping it:" xform)
      identity)))



(pipeline-interp
 '(pipeline
   (read-phase (xforms
                :unicode-recode true
                :remove-cruft true
                ))
   ;; (xform-phase (xforms
   ;;         :reify-values true foo
   ;;         :make-property-remapper false
   ;;         :make-value-type-filter false
   ;;         :make-value-synonymizer false
   ;;         :static-value-merge-fn false
   ;;         :default-value-merge-fn true
   ;;         :prune-nils true bar baz
   ;;         :denormalize-map false))
   ))
