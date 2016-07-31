(ns cloth.filters-test
  (:require
    [cloth.filters :as filters]
    [promesa.core :as p]
    [clojure.core.async :as async :refer [>! <! <!! go]]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
        :clj  [
    [clojure.test :refer [is are deftest testing use-fixtures]]])
    [cloth.chain :as chain]))



(deftest new-block-ch-test
  #?(:cljs
     (async done
       (-> (filters/block-ch)
           (p/then (fn [r]
                     (is (re-find #"TestRPC" r))
                     (done)))
           (p/catch (fn [e]
                      (is false (str "Did not return response"))
                      (done)))))
     :clj
     (let [{:keys [events stop start]} @(filters/new-block-ch)
           block-hash (<!! events)
           latest @(chain/latest-block)]
       (is (= block-hash (:hash latest)))
       (stop))))
