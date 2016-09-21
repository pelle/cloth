(ns cloth.core-test
  (:require [cloth.core :as core]
            [cloth.keys :as keys]
            [promesa.core :as p]
            [cloth.test-helpers]
            [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
        :clj  [
            [clojure.test :refer [is are deftest testing use-fixtures]]
            [cloth.contracts :as c :refer [defcontract]]])
            [cloth.tx :as tx]
            [cloth.core :as cloth])
  #?(:cljs (:require-macros
             [cloth.contracts :as c])))


(defn create-new-keypair! []
  (reset! core/global-signer (keys/create-keypair)))

(deftest test-keypair
  (reset! core/global-signer nil)
  (is (= (core/current-signer) nil))
  (let [gl (keys/create-keypair)
        lo (keys/create-keypair)]
    (reset! core/global-signer gl)
    (is (= (core/current-signer) gl))
    (binding [core/bound-signer lo]
      (is (= (core/current-signer) lo)))
    (reset! core/global-signer nil)))

(deftest balance-test
  (create-new-keypair!)
  #?(:cljs
     (async done
       (p/then (core/balance)
               (fn [b]
                 (is (= b 0))
                 (done))))
     :clj
     (is (= @(core/balance) 0)))


  #?(:cljs
     (async done
       (-> (core/faucet! 1000000)
           (p/then core/when-mined)
           (p/then #(core/balance))
           (p/then (fn [b]
                     (is (= b 1000000))
                     (done)))
           (p/catch (fn [e]
                      (println "Error: " (prn-str e))
                      (is (nil? e))
                      (done)))))
     :clj
     (do @(core/faucet! 1000000)
         (is (= @(core/balance) 1000000)))))

(deftest fetch-nonce-test
  (create-new-keypair!)
  #?(:cljs
     (async done
       (p/then (core/fetch-nonce)
               (fn [nonce]
                 (is (= nonce {:nonce 0}))
                 (done))))
     :clj
     (p/then (core/fetch-nonce)
             (fn [nonce]
               (is (= nonce {:nonce 0}))))))


(deftest fetch-gas-price-test
  (create-new-keypair!)
  #?(:cljs
     (async done
       (p/then (core/fetch-gas-price)
               (fn [price]
                 (is (= price {:gas-price 1}))
                 (done))))
     :clj
     (p/then (core/fetch-gas-price)
             (fn [price]
               (is (= price {:gas-price 1}))))))

(deftest fetch-defaults-test
  (create-new-keypair!)
  #?(:cljs
     (async done
       (p/then (core/fetch-defaults)
               (fn [defaults]
                 (is (= defaults {:gas-price 1 :nonce 0}))
                 (done))))
     :clj
     @(p/then (core/fetch-defaults)
              (fn [defaults]
                (is (= defaults {:gas-price 1 :nonce 0}))))))

(deftest sign-and-send-test
  (create-new-keypair!)
  (let [recipient (:address (keys/create-keypair))]
    #?(:cljs
       (async done
         (p/catch
           (->> (core/faucet! 10000000000)
                (p/mapcat (fn [_] (core/sign-and-send! {:to recipient :value 100000})))
                (p/mapcat
                  (fn [tx]
                    (is tx)
                    (is (= (:from tx) (:address (core/current-signer))))
                    (is (= (:to tx) recipient))
                    (is (= (:value tx) 100000))
                    (done))))
           (fn [e]
             (println "Error: " (prn-str e))
             (is (nil? e))
             (done))))
       :clj
       (let [_ @(core/faucet! 10000000000)
             tx @(core/sign-and-send! {:to recipient :value 12340000})]
         (is tx)
         (is (= (:from tx) (:address (core/current-signer))))
         (is (= (:to tx) recipient))
         (is (= (:value tx) 12340000))
         (is (= @(chain/get-balance recipient) 12340000))))))

(deftest sign-with-signer-url-test
  (let [keypair (keys/create-keypair)]
    (reset! core/global-signer {:address  (:address keypair)
                                :type     :url
                                :show-url (fn [url] (core/sign-and-send! (tx/url->map url) keypair))})
    (let [recipient (:address (keys/create-keypair))]
      #?(:cljs
         (async done
           (p/catch
             (->> (core/faucet! 10000000000)
                  (p/mapcat (fn [_] (core/sign-and-send! {:to recipient :value 100000})))
                  (p/mapcat
                    (fn [tx]
                      (is tx)
                      (is (= (:from tx) (:address (core/current-signer))))
                      (is (= (:to tx) recipient))
                      (is (= (:value tx) 100000))
                      (done))))
             (fn [e]
               (println "Error: " (prn-str e))
               (is (nil? e))
               (done))))
         :clj
         (let [_ @(core/faucet! 10000000000)
               tx @(core/sign-and-send! {:to recipient :value 12340000})]
           (is tx)
           (is (= (:from tx) (:address (core/current-signer))))
           (is (= (:to tx) recipient))
           (is (= (:value tx) 12340000))
           (is (= @(chain/get-balance recipient) 12340000)))))))

(deftest sign-with-signer-proxy-test
  (let [keypair (keys/create-keypair)]
    (reset! core/global-signer keypair)
    (c/defcontract proxy "test/cloth/Proxy.sol")

    (let [recipient (:address (keys/create-keypair))]
      #?(:cljs
         (async done
           (p/catch
             (->> (core/faucet! 10000000000)
                  (p/mapcat (fn [] (deploy-proxy!)))
                  (p/mapcat (fn [contract]
                              (reset! core/global-signer {:address  contract
                                                          :type     :proxy
                                                          :device  keypair})))
                  (p/mapcat (fn [] (core/faucet! 10000000000)))
                  (p/mapcat (fn [_] (core/sign-and-send! {:to recipient :value 12340000})))
                  (p/mapcat
                    (fn [tx]
                      (is tx)
                      (is (= (:from tx) (:address keypair)))
                      (is (= (:to tx) (:address (core/current-signer))))
                      (is (= (:value tx) 0))
                      ))
                  (p/mapcat
                    (fn [] (chain/get-balance recipient)))
                  (p/mapcat #(is (= % 12340000)))
                  (p/mapcat
                    (fn [] (chain/get-balance (:address (core/current-signer)))))
                  (p/mapcat #(is (= % (- 10000000000 12340000))))
                  (p/mapcat done))
             (fn [e]
               (println "Error: " (prn-str e))
               (is (nil? e))
               (done))))
         :clj
         (let [_ @(core/faucet! 10000000000)
               contract @(deploy-proxy!)
               _ (reset! core/global-signer {:address  contract
                                           :type     :proxy
                                           :device  keypair})
               _ @(core/faucet! 10000000000)
               tx @(core/sign-and-send! {:to recipient :value 12340000})]
           (is tx)
           (is (= (:from tx) (:address keypair)))
           (is (= (:to tx) contract))
           (is (= (:value tx) 0))
           (is (= @(chain/get-balance recipient) 12340000))
           (is (= @(chain/get-balance contract) (- 10000000000 12340000))))))))
