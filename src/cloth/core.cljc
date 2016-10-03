(ns cloth.core
  (:require [cloth.tx :as tx]
            [promesa.core :as p]
            [cats.core :as m]
            [cloth.chain :as chain]
            [cloth.util :as util]
            [cloth.bytes :as b]
            [cloth.keys :as keys]))

#?(:cljs (enable-console-print!))

;; In a browser we will typically only use one keypair
(def ^:dynamic bound-signer nil)
(defonce global-signer (atom nil))
(defonce tx-poll-rate  (atom 15000))
(defn current-signer []
  (or bound-signer @global-signer))

(defn maybe-create-keypair []
  (when-not (current-signer)
    (reset! global-signer (keys/create-keypair))))


(defn when-mined
  "Given a transaction hash it polls every 5 seconds to see if it's mined"
  ([tx-hash] (when-mined tx-hash 60))
  ([tx-hash attempts]
   ;(println "tx " tx-hash " " attempts)
   (->> (chain/get-transaction-by-hash (:hash tx-hash tx-hash))
       (p/mapcat
         (fn [tx]
           (if (:block-number tx)
             (p/promise tx)
             (if (= attempts 0)
               (p/rejected (ex-info (str "tx was not mined " tx-hash) {:tx tx}))
               (do
                 (-> (p/delay @tx-poll-rate tx-hash)
                     (p/then (fn [s]
                               (when-mined tx-hash (dec attempts)))))))))))))

(defn faucet!
  "Donate some eth from the local rpc coinbase to current account. Intended for testing purposes only."
  ([amount]
   (if (current-signer)
     (faucet! (:address (current-signer)) amount)))
  ([address amount]
   (->> (chain/coinbase)
        (p/mapcat #(chain/send-transaction {:value amount
                                            :from %
                                            :to address}))
        (p/mapcat when-mined))))

(defn balance []
  (chain/get-balance (:address (current-signer))))

(defn fetch-nonce
  ([]
   (fetch-nonce (current-signer)))
  ([kp-or-address]
   (p/then (chain/get-transaction-count (:address kp-or-address kp-or-address))
           (fn [tx-count] {:nonce tx-count}))))

(defn fetch-gas-price []
  (p/then (chain/gas-price)
          (fn [price] {:gas-price price})))

(defn fetch-defaults
  ([] (fetch-defaults (current-signer)))
  ([kp]
   (p/then (p/all [(fetch-nonce kp)
                   (fetch-gas-price)])
           #(apply merge %))))

(defn estimate-gas [t]
  (p/catch
    (p/then (chain/estimate-gas t)
                   (fn [gas]
                     (assoc t :gas-limit (* gas 3))))
    #(do
      (println "estimate gas error: " (prn-str %))
      (println ">>>>>> " (prn-str t))
      ;; Some strange error happens sometimes in testrpc
      (assoc t :gas-limit 100000))))

(defn spytx [tx]
  ;(prn tx)
  (prn (tx/tx->map tx))
  tx)

(defmulti sign-with-signer!
          "Signs a given transaction map using a given signer and returns a promise that will return a tx hash.

           The `:type` attribute of the signer map dispatches to given implementation."
          (fn [_ signer] (:type signer)))

(defmethod sign-with-signer! :default
  [t signer]
  (->>
    (fetch-defaults signer)
    (p/mapcat #(estimate-gas (merge t % {:from (:address signer)})))
    (p/mapcat
      #(-> (tx/create-and-sign % (keys/get-private-key signer))
           ;spytx
           (tx/->hex)
           (chain/send-raw-transaction)))))

;; The :url signer requires a function in :show-url which receives an ethereum url. This can be presented to a user together with an optional callback method
(defmethod sign-with-signer! :url [t {:keys [show-url]}]
  (show-url (tx/map->url t)))

;; Wraps a transaction in forward transaction being sent to a proxy contract
(defmethod sign-with-signer! :proxy [t {:keys [address device]}]
  (sign-with-signer! (assoc t
                       :to address
                       :value 0
                       :data (util/encode-fn-sig "forward"
                                                 [:address :uint256 :bytes]
                                                 [(:to t) (:value t 0) (:data t (b/->bytes ""))]))
                     device))

(defn sign-and-send!
  ([t]
   (sign-and-send! t (current-signer)))
  ([t signer]
   (->> (sign-with-signer! t signer)
        (p/mapcat when-mined))))

