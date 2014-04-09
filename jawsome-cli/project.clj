(defproject com.onekingslane.danger/jawsome-cli "1.1.0"
  :description "CLI bindings around jawsome-dsl"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [com.onekingslane.danger/jawsome-dsl "1.1.0"]
                 [org.clojure/tools.cli "0.3.1"]]
  :profiles {:dev {:dependencies [[org.slf4j/slf4j-log4j12 "1.7.5"]]
                   :resource-paths ["resources"]}})
