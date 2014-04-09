(defproject com.onekingslane.danger/jawsome-core "1.0.0"
  :description "A library of functions for dealing with reading and manipulating dirty JSON data"
  :url "http://www.github.com/okl/danger-jawsome"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.onekingslane.danger/clojure-common-utils "0.0.23"]
                 [com.onekingslane.danger/diesel "1.0.1"]
                 [com.onekingslane.danger/denormal "1.0.1"]
                 [clj-yaml "0.4.0"]
                 [cheshire "5.2.0"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-log4j12 "1.7.5"]]
                   :resource-paths ["resources"]}}
  :aliases {"deploy" ["deploy" "clojars"]})
