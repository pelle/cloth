(ns cloth.filters-test
  (:require
    [cloth.filters :as filters]
    [cloth.test-helpers]
    [cloth.core :as core]
    [cloth.keys :as keys]
    [promesa.core :as p]
    [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]
               [cljs.core.async :refer [>! <!]]]
        :clj  [
    [clojure.test :refer [is are deftest testing use-fixtures]]
    [clojure.core.async :as async :refer [>! <! <!! go go-loop]]
    [cloth.contracts :as c]])
    [cloth.util :as util]
    [cloth.bytes :as b])
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]]
             [cloth.contracts :as c])))

(defn create-new-keypair! []
  (reset! core/global-signer (keys/create-keypair)))

(deftest new-block-ch-test
  #?(:cljs
     (async done
       (p/catch
         (p/then (filters/new-block-ch)
                 (fn [{:keys [events stop start]}]
                   (go
                     (let [block-hash (<! events)]
                       (is block-hash)
                       (stop)
                       (done)))))
         (fn [e]
           (println "error " (prn-str e))
           (is (= e nil))
           (done))))
     :clj
     (let [{:keys [events stop start]} @(filters/new-block-ch)
           _ @(chain/testrpc-mine!)
           block-hash (<!! events)]
       (stop)
       (is block-hash))))

(deftest event-parser-tests
  (is (= ((filters/event-parser [{:indexed true, :name "recipient", :type :address} {:indexed false, :name :amount, :type :uint32}])
           {:address "0xc060658f11757aba1b47020108ae78126c48c050", :type "mined", :topics ["0x4e6fa9ac631a2ff1ed5799bf99b1873a35f0c0cab4314d0adf9edb8a9bf933ab" "0x000000000000000000000000e3042fbf0e5f4597144c17bb84d697ba388701fc"], :data "0x000000000000000000000000000000000000000000000000000000000000007b", :block-hash "0x2a42449f8c4f46b0add7f8248d659ffcde8f0c7d547830cabae0487ba49a317d", :tx "0x4392f22963b64a224779818317444e674c74c9310eec976692b19252b66127f6", :log-index 0, :block-number 1299, :transaction-index 0})
         {:recipient "0xe3042fbf0e5f4597144c17bb84d697ba388701fc"
          :amount    123}))
  (is (= ((filters/event-parser [{:indexed true, :name :shouter, :type :address} {:indexed false, :name :message, :type :string}])
           {:address "0xe7b9ef10c866154176cce5ac06de663c85319abb", :type "mined", :topics ["0x811f7cff0a3374ff67cccc3726035d34ba70410e0256818a891e4d6acc01d88e" "0x0000000000000000000000002ce4764c1593f4aa912c1dbcc890de47ba2a5d66"], :data "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000548656c6c6f000000000000000000000000000000000000000000000000000000", :block-hash "0x0c8514bf55eceeef82890eb30ec2af563e2630533f0b7fce17a8b9ad450ecb8f", :tx "0x2ef0a06b78d952cbe6d7995f73537cc51cf5f27a3989e81b6bde153ad7fe219d", :log-index 0, :block-number 1291, :transaction-index 0})
         {:shouter "0x2ce4764c1593f4aa912c1dbcc890de47ba2a5d66"
          :message "Hello"})))


(c/defcontract simple-token "test/cloth/SimpleToken.sol")

(deftest event-ch-tests
  (create-new-keypair!)
  #?(:cljs
     (let [recipient (:address (keys/create-keypair))
           contract (atom nil)
           event-sig (util/encode-event-sig "Issued" [:address :uint32])]
       (async done
         (-> (core/faucet! 1000000000000000000)
             (p/then core/when-mined)
             (p/then deploy-simple-token!)
             (p/then (fn [c] (reset! contract c)))
             (p/then #(filters/event-ch
                       {:address @contract
                        :topics  [event-sig]}))
             (p/then (fn  [{:keys [events stop start]}]
                       (p/then (issue!! @contract recipient 123)
                               (fn [tx]
                                 (go
                                   (let [event (<! events)]
                                     (stop)
                                     (is event)
                                     (is (= (:address event) @contract))
                                     (is (= (:tx event)))
                                     (is (= (:topics event) [event-sig (b/add0x (util/encode-solidity :address recipient))]))
                                     (is (= (:data event) (b/add0x (util/encode-solidity :uint32 123))))
                                     (done)
                                     ))))))
             (p/catch (fn [e]
                        (println "Error: " (prn-str e))
                        (prn (.-stack e))
                        (done))))))
     :clj
     (let [state @(chain/testrpc-snapshot!)]
       @(core/faucet! 1000000000000000000)
         (let [contract @(deploy-simple-token!)
               recipient (:address (keys/create-keypair))
               event-sig (util/encode-event-sig "Issued" [:address :uint32])
               {:keys [events stop start]} @(filters/event-ch
                                              {:address contract
                                               :topics  [event-sig]})
               tx @(issue!! contract recipient 123)
               event (<!! events)]
           (stop)
           (is event)
           (is (= (:address event) contract))
           (is (= (:tx event)))
           (is (= (:topics event) [event-sig (b/add0x (util/encode-solidity :address recipient))]))
           (is (= (:data event) (b/add0x (util/encode-solidity :uint32 123))))
           @(chain/testrpc-revert! state)
           ))))
