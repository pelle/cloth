(ns cloth.core-test
  (:require [cloth.core :as core]
            [cloth.keys :as keys]
            [promesa.core :as p]
            [cloth.chain :as chain]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))


(defn create-new-keypair! []
  (reset! core/global-keypair (keys/create-keypair)))

(deftest test-keypair
  (reset! core/global-keypair nil)
  (is (= (core/keypair) nil))
  (let [gl (keys/create-keypair)
        lo (keys/create-keypair)]
    (reset! core/global-keypair gl)
    (is (= (core/keypair) gl))
    (binding [core/bound-keypair lo]
      (is (= (core/keypair) lo)))
    (reset! core/global-keypair nil)))

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
                    (is (= (:from tx) (:address (core/keypair))))
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
         (is (= (:from tx) (:address (core/keypair))))
         (is (= (:to tx) recipient))
         (is (= (:value tx) 12340000))
         (is (= @(chain/get-balance recipient) 12340000))))))
