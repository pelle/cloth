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
(defn ethrpc [& args]
  (apply net/rpc @ethereum-rpc args))

(defn client-version []
  (ethrpc "web3_clientVersion"))

(defn gas-price []
  (-> (ethrpc "eth_gasPrice")
      (p/then util/hex->uint)))

(defn block-number []
  (-> (ethrpc "eth_blockNumber")
      (p/then util/hex->uint)))

(defn get-balance
  ([address] (get-balance address "latest"))
  ([address block-number]
   (-> (ethrpc "eth_getBalance" address block-number)
       (p/then util/hex->uint))))

(defn get-transaction-count
  ([address] (get-transaction-count address "latest"))
  ([address block-number]
   (-> (ethrpc "eth_getTransactionCount" address block-number)
       (p/then util/hex->uint))))

(defn rpc->tx [tx]
  (if tx
    (-> (select-keys tx [:from :to :hash :input])
        (assoc :value (util/hex->uint (:value tx))
               :block-hash (:blockHash tx)
               :block-number (and (:blockNumber tx) (util/hex->uint (:blockNumber tx)))
               :nonce (util/hex->uint (:nonce tx))
               :gas (util/hex->uint (:gas tx))
               :gas-price (util/hex->uint (:gasPrice tx))
               :transaction-index (util/hex->uint (:transactionIndex tx))))))

(defn receipt->tx [tx]
  (if tx
    {:hash (:transactionHash tx)
     :transaction-index (util/hex->uint (:transactionIndex tx))
     :block-hash (:blockHash tx)
     :block-number (util/hex->uint (:blockNumber tx))
     :contract-address (:contractAddress tx)
     :cumulative-gas-used (util/hex->uint (:cumulativeGasUsed tx))
     :gas-used (util/hex->uint (:gasUsed tx))
     :logs (:logs tx)}))

(defn rpc->event [e]
  (-> (select-keys e [:address :type :topics :data])
      (assoc :block-hash (:blockHash e)
             :tx (:transactionHash e)
             :log-index (util/hex->uint (:logIndex e))
             :block-number (util/hex->uint (:blockNumber e))
             :transaction-index (util/hex->uint (:transactionIndex e)))))

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
               :number            (util/hex->uint (:number block))
               :difficulty        (util/hex->uint (:difficulty block))
               :gas-used          (util/hex->uint (:gasUsed block))
               :nonce             (util/hex->uint (:nonce block))
               :gas-limit         (util/hex->uint (:gasLimit block))
               :total-difficulty  (util/hex->uint (:totalDifficulty block))
               :timestamp         (coerce/from-long (* 1000 (long (util/hex->uint (:timestamp block)))))))))

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

(defn latest-block []
  (->> (block-number)
       (p/mapcat get-block-by-number)))

(defn get-transaction-by-hash
  [hash]
  (p/then (ethrpc "eth_getTransactionByHash" hash)
          rpc->tx))

(defn get-transaction-by-block-number-and-index
  [block-number index]
  (p/then (ethrpc "eth_getTransactionByBlockNumberAndIndex" block-number index)
          rpc->tx))

(defn get-transaction-receipt
  [tx-or-hash]
  (let [hash (:hash tx-or-hash tx-or-hash)]
    (p/then (ethrpc "eth_getTransactionReceipt" hash)
            receipt->tx)))

(defn get-storage-at
  ([address index]
    (get-storage-at address index "latest"))
  ([address index block-number]
   (ethrpc "eth_getStorageAt" address (util/add0x (util/int->hex index)) block-number)))

(defn get-code
  [address block-number]
  (ethrpc "eth_getCode" address block-number))

(defn send-raw-transaction
  [data]
  (ethrpc "eth_sendRawTransaction" data))

(defn estimate-gas
  ([params]
   (-> (ethrpc "eth_estimateGas" (select-keys params [:from :to :data :value]))
       (p/then util/hex->uint))))

(defn call
  ([object] (call object "latest"))
  ([object block-number]
   (ethrpc "eth_call" object block-number)))

(defn get-filter-changes
  [id]
  (ethrpc "eth_getFilterChanges" id))

(defn get-filter-logs
  [id]
  (ethrpc "eth_getFilterLogs" id))

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

(defn filter-changes
  ([id]
   (filter-changes identity id))
  ([formatter id]
   (p/then (ethrpc "eth_getFilterChanges" id)
           formatter)))

(defn get-logs
  ([query]
   (get-logs identity query))
  ([formatter query]
   (p/then (ethrpc "eth_getLogs" query)
           formatter)))

(defn new-filter
  [query]
  (ethrpc "eth_newFilter" query))

(defn new-block-filter
  []
  (ethrpc "eth_newBlockFilter"))

(defn uninstall-filter [id]
  (ethrpc "eth_uninstallFilter" id))

