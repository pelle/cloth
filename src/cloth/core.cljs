(ns cloth.core
  (:require [cloth.tx :as tx]
            [promesa.core :as p]
            [cats.core :as m]
            [cloth.chain :as chain]
            [cloth.util :as util]
            [cloth.keys :as keys]))

;; In a browser we will typically only use one keypair
(defonce keypair (atom nil))

(defn maybe-create-keypair []
  (when-not @keypair
    (reset! keypair (keys/create-keypair))))

(defn faucet!
  "Donate some eth from the local rpc coinbase to current account"
  ([amount]
   (if @keypair
      (faucet! (:address @keypair) amount)))
  ([address amount]
   (p/then (chain/coinbase)
           #(chain/send-transaction {:value amount
                                     :from %
                                     :to address}))))

(defn fetch-nonce
  ([]
   (fetch-nonce @keypair))
  ([kp-or-address]
   (p/then (chain/get-transaction-count (:address kp-or-address kp-or-address))
           (fn [tx-count] (p/promise {:nonce (inc tx-count)})))))

(defn fetch-gas-price []
  (p/then (chain/gas-price)
          (fn [price] (p/promise {:gasPrice price}))))

(defn fetch-defaults
  ([] (fetch-defaults @keypair))
  ([kp]
   (p/then (p/all [(fetch-nonce kp)
                   (fetch-gas-price)])
           #(apply merge %))))

(defn when-mined
  "Given a transaction hash it polls every 5 seconds to see if it's mined"
  [tx-hash]
  (p/chain (p/delay 5000)
           #(chain/get-transaction-by-hash tx-hash)
           (fn [tx]
             (if (tx "blockHash")
               tx
               (when-mined tx-hash)))))

(defn sign-and-send!
  ([t]
   (sign-and-send! t @keypair))
  ([t kp]
   (p/then (fetch-defaults kp)
           #(-> (merge t %)
                (tx/create-tx)
                (tx/sign-tx kp)
                (tx/hex)
                (chain/send-raw-transaction)))))


