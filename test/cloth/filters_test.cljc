(ns cloth.filters-test
  (:require
    [cloth.filters :as filters]
    [promesa.core :as p]
    [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]
               [cljs.core.async :refer [>! <!]]]
        :clj  [
    [clojure.test :refer [is are deftest testing use-fixtures]]
    [clojure.core.async :as async :refer [>! <! <!! go go-loop]]]))
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]])))



(deftest new-block-ch-test
  #?(:cljs
     (async done
       (println "new-block-ch")
       (p/catch
         (p/then (filters/new-block-ch)
                 (fn [{:keys [events stop start]}]
                   (go
                     (let [block-hash (<! events)]
                       (p/catch
                         (p/then (chain/latest-block)
                                 (fn [latest]
                                   (is (= block-hash (:hash latest)))
                                   (stop)
                                   (done)))
                         (fn [e]
                           (println "error " (prn-str e))
                           (is (= e nil))
                           (stop)
                           (done)))))))
         (fn [e]
           (println "error " (prn-str e))
           (is (= e nil))
           (stop)
           (done))))
     :clj
     (let [{:keys [events stop start]} @(filters/new-block-ch)
           block-hash (<!! events)
           latest @(chain/latest-block)]
       (is (= block-hash (:hash latest)))
       (stop))))
