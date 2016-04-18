(ns cloth.core-test
  (:require [cloth.core :as core]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

(deftest test-home
  (is (= true true)))

