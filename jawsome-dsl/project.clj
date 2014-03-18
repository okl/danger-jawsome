(defproject jawsome-dsl "0.3-SNAPSHOT"
  :description "DSL for jawsome pipelines"
  :url "http://www.github.com/okl/danger-jawsome"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.onekingslane.danger/diesel "1.0.3"]
                 [com.onekingslane.danger/clojure-common-utils "0.0.23"]
                 [com.onekingslane.danger/jawsome-core "0.3-SNAPSHOT"]
                 [com.onekingslane.danger/jsonschema "1.0.1"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-log4j12 "1.7.5"]]
                   :resource-paths ["resources"]}}
  :main jawsome-dsl.core)
