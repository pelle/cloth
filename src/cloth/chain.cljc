(ns cloth.chain
  (:require [promesa.core :as p]
            [cloth.net :as net]
            [cuerdas.core :as c]
            [cloth.tx :as tx]
            [cloth.util :as util]
    #?@(:cljs [[cljs-time.coerce :as coerce]])
    #?@(:clj  [[clj-time.coerce :as coerce]])))

#?(:clj
   (def clj->js identity))

(defonce ethereum-rpc (atom "http://localhost:8545/"))
(def ethrpc (partial net/rpc (deref ethereum-rpc)))

(defn client-version []
  (ethrpc "web3_clientVersion"))

(defn gas-price []
  (-> (ethrpc "eth_gasPrice")
      (p/then util/hex->int)))

(defn block-number []
  (-> (ethrpc "eth_blockNumber")
      (p/then util/hex->int)))

(defn get-balance
  ([address] (get-balance address "latest"))
  ([address block-number]
   (-> (ethrpc "eth_getBalance" address block-number)
       (p/then util/hex->int))))

(defn get-transaction-count
  ([address] (get-transaction-count address "latest"))
  ([address block-number]
   (-> (ethrpc "eth_getTransactionCount" address block-number)
       (p/then util/hex->int))))

(defn rpc->tx [tx]
  (if tx
    (-> (select-keys tx [:from :to :hash])
        (assoc :value (util/hex->int (:value tx))
               :block-hash (:blockHash tx)
               :block-number (util/hex->int (:blockNumber tx))
               :nonce (util/hex->int (:nonce tx))
               :gas (util/hex->int (:gas tx))
               :gas-price (util/hex->int (:gasPrice tx))
               :transaction-index (util/hex->int (:transactionIndex tx))
               :input (util/hex->int (:input tx))))))

(defn receipt->tx [tx]
  (if tx
    {:hash (:transactionHash tx)
     :transaction-index (util/hex->int (:transactionIndex tx))
     :block-hash (:blockHash tx)
     :block-number (util/hex->int (:blockNumber tx))
     :contract-address (:contractHash tx)
     :cumulative-gas-used (util/hex->int (:cumulativeGasUsed tx))
     :gas-used (util/hex->int (:gasUsed tx))
     :logs (:logs tx)}))

(defn rpc->block [block]
  (if block
    (-> (select-keys block [:hash :miner :uncles])
        (assoc :transactions-root (:transactionsRoot block)
               :logs-bloom        (:logsBloom block)
               :state-root        (:stateRoot block)
               :parent-hash       (:parentHash block)
               :receipt-root      (:receiptRoot block)
               :sha3-uncles       (:sha3uncles block)
               :extra-data        (:extraData block)
               :transactions      (mapv rpc->tx (:transactions block))
               :number            (util/hex->int (:number block))
               :difficulty        (util/hex->int (:difficulty block))
               :gas-used          (util/hex->int (:gasUsed block))
               :nonce             (util/hex->int (:nonce block))
               :gas-limit         (util/hex->int (:gasLimit block))
               :total-difficulty  (util/hex->int (:totalDifficulty block))
               :timestamp         (coerce/from-long (* 1000 (long (util/hex->int (:timestamp block)))))))))

(defn get-block-by-hash
  ([hash]
   (get-block-by-hash hash false))
  ([hash full-transactions?]
   (p/then (ethrpc "eth_getBlockByHash" hash full-transactions?)
           rpc->block)))

(defn get-block-by-number
  ([num]
   (get-block-by-number num false))
  ([num full-transactions?]
   (p/then (ethrpc "eth_getBlockByNumber" num full-transactions?)
           rpc->block)))

(defn get-transaction-by-hash
  [hash]
  (p/then (ethrpc "eth_getTransactionByHash" hash)
          rpc->tx))

(defn get-transaction-receipt
  [hash]
  (p/then (ethrpc "eth_getTransactionReceipt" hash)
          receipt->tx))

(defn get-storage-at
  [index block-number]
  (ethrpc "eth_getStorageAt" index block-number))

(defn get-code
  [address block-number]
  (ethrpc "eth_getCode" address block-number))

(defn send-raw-transaction
  [data]
  (ethrpc "eth_sendRawTransaction" data))

(defn call
  ([object] (call object "latest"))
  ([object block-number]
   (ethrpc "eth_call" object block-number)))

(defn new-filter
  [object]
  (ethrpc "eth_newFilter" (clj->js object)))

(defn new-block-filter
  []
  (ethrpc "eth_newBlockFilter"))

(defn get-filter-changes
  [id]
  (ethrpc "eth_getFilterChanges" id))

(defn get-filter-logs
  [id]
  (ethrpc "eth_getFilterLogs" id))

(defn get-logs
  [object]
  (ethrpc "eth_getLogs" (clj->js object)))

(defn uninstall-filter
  [id]
  (ethrpc "eth_uninstallFilter" id))

;; the following are just for local development we add signing in the browser
(defn send-transaction
  "Only use this for local development not for prod"
  [data]
  (ethrpc "eth_sendTransaction" (clj->js data)))

(defn accounts
  "Returns accounts available in local rpc. Do not use in real code"
  []
  (ethrpc "eth_accounts"))

(defn coinbase
  "Returns accounts available in local rpc. Do not use in real code"
  []
  (ethrpc "eth_coinbase"))
