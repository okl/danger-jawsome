;; # Criteria for lines we even want to try to read from a file.

;; ## Remove any text preceding JSON
;; If the lines of the file have data that looks like
;; `apache-store-json_ssl.log.2:{"a":"b", ...}`
;; get rid of the leading markup so they come out as
;; `{"a":"b", ...}`
;;
(defn- line-starts-with-garbage?
  "Returns true if the line doesn't start with a '{'"
  [log-line-string]
  (not (= (get log-line-string 0) \{)))

(defn- line-ends-with-garbage?
  "Returns true if the line doesn't start with a '{'"
  [log-line-string]
  (let [end-pos (- (count log-line-string) 1)]
    (not (= (get log-line-string end-pos) \}))))

(defn remove-extraenous-line-markup [log-line-string]
  (if (or (line-starts-with-garbage? log-line-string)
          (line-ends-with-garbage? log-line-string))
    (let [the-line-less-garbage (re-find #"\{.*\}" log-line-string)]
      (or the-line-less-garbage
          (and (print-expr
                (str "This line seems to be garbage, doesn't start with '{', or end with '}':"
                     log-line-string))
               ;; throw out any garbage lines.
               ;; It would be better to throw an exception
               ;; but because we have this code running
               ;; inside the jsonschema code... well...
               ;; Refactor some day
               "")))
    log-line-string))


;; should handle comments and skipping lines with line comments
