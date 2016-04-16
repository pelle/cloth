(ns cloth.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [cloth.core :as core]))

(deftest test-home
  (is (= true true)))

