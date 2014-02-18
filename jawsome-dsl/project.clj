(defproject jawsome-dsl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.onekingslane.danger/diesel "1.0.1"]
                 [com.onekingslane.danger/clojure-common-utils "0.0.22"]
                 [com.onekingslane.danger/jawsome-core "0.1.1-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-log4j12 "1.7.5"]]
                   :resource-paths ["resources"]}}
  :main jawsome-dsl.core)
