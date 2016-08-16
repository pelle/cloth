(ns cloth.contracts-test
  (:require [cloth.core :as core]
            [cloth.keys :as keys]
            [promesa.core :as p]
            [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]
               [cloth.contracts :as c]
               [cljs.core.async :refer [>! <!]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]
               [cloth.contracts :as c :refer [defcontract]]
               [clojure.core.async :as async :refer [>! <! <!! go go-loop]]]))
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]]
             [cloth.contracts :as c])))

(defn create-new-keypair! []
  (reset! core/global-keypair (keys/create-keypair)))

#?(:clj
   (deftest compile-solidity-test
     (let [info (c/compile-solidity "test/cloth/SimpleToken.sol")]
       (is (= (keys info) '(:contracts :version))))
     ))

(c/defcontract simple-token "test/cloth/SimpleToken.sol")

(deftest deploy-contract-test
  (create-new-keypair!)
  #?(:cljs
     (let [recipient (:address (keys/create-keypair))
           contract (atom nil)]
       (async done
         (-> (core/faucet! 10000000000)
             (p/then core/when-mined)
             (p/then deploy-simple-token!)
             (p/then (fn [c] (reset! contract c)))
             (p/then #(issuer @contract))
             (p/then (fn [result]
                       (is (= result (:address (core/keypair))))))
             (p/then #(circulation @contract))
             (p/then (fn [result]
                       (is (= result 0))))
             (p/then #(issue? @contract recipient 123))
             (p/then (fn [result]
                       (is result)))
             (p/then #(customer @contract recipient))
             (p/then (fn [result]
                       (is (= result {:authorized-time 0 :balance 0}))))

             (p/then #(issue!! @contract recipient 123))
             (p/then (fn [result]
                       (is result)))
             (p/then #(circulation @contract))
             (p/then (fn [result]
                       (is (= result 123))))
             (p/then #(balances @contract recipient))
             (p/then (fn [result]
                       (is (= result 123))))
             (p/then #(message-ch @contract))
             (p/then (fn [{:keys [events stop start] :as c}]
                       (p/then (set-message!! @contract "Hello")
                               #(go
                                 (let [event (<! events)]
                                   (stop)
                                   (is (= event {:message "Hello" :shouter (:address (core/keypair))}))
                                   (done))))))
             (p/catch (fn [e]
                        (println "Error: " (prn-str e))
                        (prn (.-stack e))
                        (done))))))
     :clj
     (do @(core/faucet! 10000000000)
         (let [ contract @(deploy-simple-token!)
                recipient (:address (keys/create-keypair))]
            (is contract)
            (is (= @(issuer contract) (:address (core/keypair))))
            (is (= @(circulation contract) 0))
            (is (= @(issue? contract recipient 123) true))
            (is (= @(customer contract recipient) {:authorized-time 0 :balance 0}))
            (is (= @(message contract) ""))
            (let [{:keys [events stop start] :as c} @(message-ch contract)
                  tx @(set-message!! contract "Hello")
                  event (<!! events)]
              (stop)
              (is (= event {:message "Hello" :shouter (:address (core/keypair))})))

            (let [ tx @(issue!! contract recipient 123)]
              (is tx)
              (is (= @(circulation contract) 123))
              (is (= @(balances contract recipient) 123))
              (is (= @(customer contract recipient) {:authorized-time 0 :balance 123}))
              (let [tx @(->> (authorize! contract recipient)
                             (p/mapcat core/when-mined))
                    authtime @(authorized contract recipient)]
                (is tx)
                (is (= @(circulation contract) 123))
                (is (= @(balances contract recipient) 123))
                (is (= @(customer contract recipient) {:authorized-time authtime :balance 123}))))
           ))))
