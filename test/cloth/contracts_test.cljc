(ns cloth.contracts-test
  (:require [cloth.core :as core]
            [cloth.keys :as keys]
            [promesa.core :as p]
            [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]
               [cloth.contracts :as c :refer-macros [defcontract]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]
               [cloth.contracts :as c :refer [defcontract] ]])))

(defn create-new-keypair! []
  (reset! core/global-keypair (keys/create-keypair)))


#?(:clj
   (deftest compile-solidity-test
     (let [info (c/compile-solidity "test/cloth/SimpleToken.sol")]
       (is (= (keys info) '(:contracts :version))))
     ))

(defcontract simple-token "test/cloth/SimpleToken.sol")

(deftest deploy-contract-test
  (create-new-keypair!)
  #?(:cljs
     (async done
       (-> (core/faucet! 10000000000)
           (p/then core/when-mined)
           (p/then deploy-simple-token!)
           (p/then (fn [b]
                     (is (= b 1000000))
                     (done)))
           (p/catch (fn [e]
                      (println "Error: " (prn-str e))
                      (is (nil? e))
                      (done)))))
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
            (let [tx @(set-message!! contract "Hello")]
              ;(prn tx)
              (is (= @(message contract) "Hello")))

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
