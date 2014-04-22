(ns jawsome-core.xform.xforms.log-test
  "Implements xform pipeline step: log tests"
  {:author "Matt Halverson"
   :date "2014/04/20"}
  (:require [clojure.test :refer :all])
  (:require [jawsome-core.xform.xforms.log :refer [log]]))

(def sample {:a 5, :b 6})

(deftest log-test
  (testing "Only thing to test is, does the logging function return what
you give it"
    (is (= (log sample)
           sample ))
    (is (= (log :debug sample)
           sample))))
