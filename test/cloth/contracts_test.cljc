(ns cloth.contracts-test
  (:require [cloth.core :as core]
            [cloth.keys :as keys]
            [cloth.test-helpers]
            [promesa.core :as p]
            [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]
               [cloth.contracts :as c]
               [cljs.core.async :refer [>! <!]]]
        :clj  [
            [clojure.test :refer [is are deftest testing use-fixtures]]
            [cloth.contracts :as c :refer [defcontract]]
            [clojure.core.async :as async :refer [>! <! <!! go go-loop]]])
            [cloth.tx :as tx])
  #?(:cljs (:require-macros
             [cljs.core.async.macros :refer [go go-loop]]
             [cloth.contracts :as c])))

(defn create-new-keypair! []
  (reset! core/global-signer (keys/create-keypair)))

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
         (-> (core/faucet! 1000000000000000000)
             (p/then core/when-mined)
             (p/then deploy-simple-token!)
             (p/then (fn [c] (reset! contract c)))
             (p/then #(issuer @contract))
             (p/then (fn [result]
                       (is (= result (:address (core/current-signer))))))
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
                                   (is (= event {:message "Hello" :shouter (:address (core/current-signer))}))
                                   (done))))))
             (p/catch (fn [e]
                        (println "Error: " (prn-str e))
                        (prn (.-stack e))
                        (done))))))
     :clj
     (let [state @(chain/testrpc-snapshot!)]
         @(core/faucet! 1000000000000000000)
         (let [ contract @(deploy-simple-token!)
                recipient-kp (keys/create-keypair)
                recipient (:address recipient-kp) ]
            (is contract)
            (is (= @(issuer contract) (:address (core/current-signer))))
            (is (= @(circulation contract) 0))
            (is (= @(get-customers contract) []))
            (is (= @(issue? contract recipient 123) true))
            (is (= @(customer contract recipient) {:authorized-time 0 :balance 0}))
            (is (= @(message contract) ""))
            (let [{:keys [events stop start] :as c} @(message-ch contract)
                  tx @(set-message!! contract "Hello")
                  event (<!! events)]
              (stop)
              (is (= event {:message "Hello" :shouter (:address (core/current-signer))})))

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
                (is (= @(get-customers contract) [recipient]))
                (is (= @(customer contract recipient) {:authorized-time authtime :balance 123}))))

            (reset! core/global-signer {:address  recipient
                                        :type     :url
                                        :show-url (fn [url]
                                                    ;(println url)
                                                    (core/sign-and-send! (tx/url->map url) recipient-kp))})
            @(core/faucet! 1000000000000000000)
            (let [other-user (:address (keys/create-keypair))
                  {:keys [events stop start] :as c} @(transferred-ch contract)
                  tx @(transfer!! contract other-user 11)
                  event (<!! events)]
              (stop)
              (is (= event {:sender recipient :recipient other-user :amount 11})))
            @(chain/testrpc-revert! state)))))


(c/defcontract constructed "test/cloth/Constructed.sol")

(deftest deploy-with-constructor-test
  (create-new-keypair!)
  #?(:cljs
     (let [contract (atom nil)]
       (async done
         (-> (core/faucet! 1000000000000000000)
             (p/then core/when-mined)
             (p/then #(deploy-constructed! "Hello"))
             (p/then (fn [c] (reset! contract c)))
             (p/then #(status %))
             (p/then (fn [message]
                       (is (= message "Hello"))
                       (done)))
             (p/catch (fn [e]
                        (println "Error: " (prn-str e))
                        (prn (.-stack e))
                        (done)))
             )))
     :clj
     (let [state @(chain/testrpc-snapshot!)]
       @(core/faucet! 1000000000000000000)
         (let [ contract @(deploy-constructed! "Hello")]
           (is contract)
           (is (= @(status contract) "Hello"))
           @(chain/testrpc-revert! state)
           ))))

