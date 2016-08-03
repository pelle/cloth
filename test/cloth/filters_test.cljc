(ns cloth.filters-test
  (:require
    [cloth.filters :as filters]
    [cloth.core :as core]
    [cloth.keys :as keys]
    [promesa.core :as p]
    [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]
               [cljs.core.async :refer [>! <!]]]
        :clj  [
    [clojure.test :refer [is are deftest testing use-fixtures]]
    [clojure.core.async :as async :refer [>! <! <!! go go-loop]]])
    [cloth.contracts :refer [defcontract]]
    [cloth.util :as util])
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]]
             [cloth.contracts :refer [defcontract]])))

(defn create-new-keypair! []
  (reset! core/global-keypair (keys/create-keypair)))

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

(defcontract simple-token "test/cloth/SimpleToken.sol")

(deftest event-ch-tests
  (create-new-keypair!)
  #?(:clj
     (do @(core/faucet! 10000000000)
         (let [contract @(deploy-simple-token!)
               recipient (:address (keys/create-keypair))
               event-sig (util/encode-event-sig "Issued" [:address :uint32])
               {:keys [events stop start]} @(filters/event-ch
                                              {:address contract
                                               :topics  [event-sig]})
               tx @(issue!! contract recipient 123)
               event (<!! events)
               ]
           (prn event)
           (is event)
           (is (= (:address event) contract))
           (is (= (:tx event) ))
           (is (= (:topics event) [event-sig (util/add0x (util/encode-solidity :address recipient))]))
           (is (= (:data event) (util/add0x (util/encode-solidity :uint32 123))))
           ))))
