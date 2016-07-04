(ns cloth.contracts-test
  (:require [cloth.core :as core]
            [cloth.keys :as keys]
            [promesa.core :as p]
            [cloth.chain :as chain]
            [cloth.contracts :refer :all]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

(defn create-new-keypair! []
  (reset! core/global-keypair (keys/create-keypair)))


#?(:clj
   (deftest compile-solidity-test
     (let [info (compile-solidity "test/cloth/SimpleToken.sol")]
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
         (let [ contract @(deploy-simple-token!)]
            (is contract)
            (is (= @(issuer contract) (:address (core/keypair))))

           )
         )))

