(defproject jawsome-core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [roxxi/clojure-common-utils "0.0.18"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-yaml "0.4.0"]
                 [cheshire "5.2.0"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-log4j12 "1.7.5"]]
                   :resource-paths ["resources"]}})
