(defproject jawsome-core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [roxxi/clojure-common-utils "0.0.16"]
                 [clj-yaml "0.4.0"]
                 [cheshire "5.2.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-simple "1.7.5"]]}})
