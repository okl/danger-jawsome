(ns jawsome-cli.example
  "Usage example for jawsome-cli.core"
  {:author "Matt Halverson"
   :date "2014/03/19"}
  (:require [roxxi.utils.print :refer [print-expr]])
  (:require [jawsome-cli.core :as cli]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (def-cli-pipeline ;; this is the top level CLI invocation, also a macro

;; this indicates that we need to evaluate this pipeline and generate
;; a main method that dispatches to the returned functions by a token...
;; for example, suppose this project uberjared up generates
;; myjar.jar
;; this file is in the namespace mycode.cli-stuff
;; our main method would support the following invocations:
;; java -jar myjar.jar -m mycode.cli-stuff denorm (*1)
;; java -jar myjar.jar -m mycode.cli-stuff schema (*2)
;; java -jar myjar.jar -m mycode.cli-stuff project (*3)
;; s.t. I could then
;; $ cat foo.js | tee xform.js *1 ... | *2 ... > schema.clj ; *3 xform.js schema.clj ... > project.xsv

;; the ... should be able to support arbitrary args supplied by the caller of the program, and we
;; need to give them the ability to layer in their own preflight and postflight functions to be called
;; around the invocations in main. This can be part of the def-cli etc...
;; these functions might intialize DB stuff, loggers, etc.
;;
;; look at clojure-hadoop to see how this type of extensibility is generated through
;; its configuration of defjob (https://github.com/alexott/clojure-hadoop/blob/master/src/clojure_hadoop/defjob.clj)

;; if someone wants to combine xform + schema for the sake of performance, they should be able
;; to do that by specifying it in the '(pipeline ...) and then passing in a command-line option
;; to *1, where they specify the schema-output-path (schema.clj in the above)

;;     (pipeline
;;      (denorm-phase ...)
;;      (schema-phase ...)
;;      (project-phase ...)))

(def my-pipeline
  '(pipeline
    (denorm-phase (read-phase (xforms :read-json))
                  (xform-phase (xforms :reify :denorm)))
    (schema-phase)
    (project-phase (delimiter "|"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Top-level:

;; for now
;; (def-cli-pipeline my-pipeline)

;; goal
;; (def-cli-pipeline
;;   :pipeline my-pipeline
;;   :log-to "/an/absolute/path"
;;   :db-cnxn "path/to/yml/file")



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; end user perspective

;; (defjob job
;;   :map my-map
;;   :map-setup my-map-setup
;;   :map-reader wrap/string-map-reader
;;   :map-writer text-writer
;;   :reduce-tasks 0
;;   :input-format :text
;;   :output-format :text
;;   :compress-output true
;;   :output-compressor "gzip")

;; (def my-pipeline
;;   '(pipeline
;;     (xform-phase ...)))

;; (def-cli-pipeline
;;   :pipeline my-pipeline
;;   :log-to "/an/absolute/path"
;;   :db-cnxn "path/to/yml/file")
