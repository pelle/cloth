(ns cloth.tx
  (:require #?@(:cljs [ethereumjs-tx])
    [cloth.util :as util]
    [clojure.walk :refer [keywordize-keys]]
    [cloth.keys :as keys]
    [cuerdas.core :as c]
    [cemerick.url :as url])
  #?(:clj
     (:import [org.ethereum.core Transaction])))

#?(:cljs
   (def Tx (aget util/eth-js "Tx")))

(defn map->tx [params]
  (reduce #(assoc % (keyword (c/camel (name (key %2))))
                    (if (= (key %2) :function)
                      (val %2)
                      (util/add0x (val %2))))
          {} params))

(defn map->url [params]
  (if (:to params)
    (let [to (:to params)
          params (select-keys params [:value :data :function :label :gas-limit :callback_url])
          params (if (:function params)
                   (dissoc params :data)
                   params)
          params (if (:data params)
                   (assoc (dissoc params :data) :bytecode (:data params))
                   params)
          params (if (:gas-limit params)
                   (assoc (dissoc params :gas-limit) :gas (:gas-limit params))
                   params)
          query-string (url/map->query params)]
      (str "ethereum:" to (if query-string (str "?" query-string))))))

(defn function-param->fnsig
  "Converts a function parameter of format name(type param, type param) into an solidity function encoding

  This is not at all complete and is primarily intended for testing simple use cases. It will likely break with even the simplest use case"
  [data]
  (if-let [[_ name args] (re-find #"([^\(]+)\((.*)\)$" data)]
    (let [args (map c/split (c/split args #"\s?,\s?"))
          types (map #(keyword (first %)) args)
          args (map last args)]
      (util/encode-fn-sig name types args))))

(defn url->map [url]
  (if-let [result (and url (re-find #"ethereum:(0x[0-9a-f]*)(\?(.*))?" url))]
    (let [params (keywordize-keys (merge {:to (get result 1)} (url/query->map (get result 3))))
          params (if (:bytecode params)
                   (assoc (dissoc params :bytecode) :data (:bytecode params))
                   params)
          params (if (:function params)
                   (assoc params :data (function-param->fnsig (:function params)))
                   params)
          params (if (:gas params)
                   (assoc (dissoc params :gas) :gas-limit (util/parse-int (:gas params)))
                   params)
          params (if (:value params)
                   (assoc params :value (util/parse-int (:value params)))
                   params)]
      params
      )))

(defn create [params]
  #?(:cljs
     (Tx. (clj->js (map->tx params))))
  #?(:clj
     (let [{:keys [to value nonce gas-price gas-limit data] :as tx} params]
       (Transaction. (if nonce (util/int->b nonce))
                     (if gas-price (util/int->b gas-price))
                     (if gas-limit (util/int->b gas-limit))
                     (if to (util/hex-> to))
                     (if value (util/int->b value))
                     (if data (util/hex-> data))))))

(defn recipient [tx]
  (->
    #?(:cljs (if (> (.-length (.-to tx)) 0) (.-to tx) (util/zeros 1)))
    #?(:clj (.getReceiveAddress tx))
    (util/->hex)
    (util/add0x)))

(defn sender [tx]
  (-> #?(:cljs (if (.verifySignature tx)
                 (.getSenderAddress tx)))
      #?(:clj (if (.getSignature tx)
                (.getSender tx)))
      (util/->hex)
      (util/add0x)))

(defn data [tx]
  (-> #?(:cljs (if (> (.-length (.-data tx)) 0) (.-data tx)))
      #?(:clj (.getData tx))
      (util/->hex)
      (util/add0x)))

(defn nonce [tx]
  (-> #?(:cljs (.-nonce tx))
      #?(:clj (.getNonce tx))
      (util/b->uint)))

(defn gas-price [tx]
  (->
    #?(:cljs (.-gasPrice tx))
    #?(:clj (.getGasPrice tx))
    (util/b->uint)))

(defn gas-limit [tx]
  (-> #?(:cljs (.-gasLimit tx))
      #?(:clj (.getGasLimit tx))
      (util/b->uint)))

(defn value [tx]
  (-> #?(:cljs (.-value tx))
      #?(:clj (.getValue tx))
      (util/b->uint)))

(defn tx->map [tx]
  (when tx
    (let [m {:to        (recipient tx)
             :data      (data tx)
             :nonce     (nonce tx)
             :gas-price (gas-price tx)
             :gas-limit (gas-limit tx)
             :value     (value tx)}]
      (if-let [from (sender tx)]
        (assoc m :from from)
        m))))

(defn tx->b [tx]
  #?(:cljs
     (.serialize tx))
  #?(:clj
     (.getEncoded tx)))

(defn ->hex [tx]
  (-> tx
      (tx->b)
      (util/->hex)
      (util/add0x)))

(defn sign [tx priv]
  #?(:cljs
     (.sign tx (keys/get-private-key priv)))
  #?(:clj
     (.sign tx (.toByteArray (.getPrivKey priv))))
  tx)

(defn create-and-sign
  "Convenience function create and sign a tx in one go"
  [m priv]
  ;(println "====== " (prn-str m))
  (-> (create m)
      (sign priv)))

(defn fn-tx
  ([contract fn-abi args]
   (let [params (if (map? (last args)) (last args) {})
         args (if (map? (last args)) (rest args) args)
         types (map :type (:inputs fn-abi))]
     (fn-tx contract (:name fn-abi) types args params)))
  ([contract name types args params]
   (-> params
       (assoc :data (util/encode-fn-sig name types args)
              :function (util/encode-fn-param name types args)
              :to contract)
       (map->tx))))

(defn create-contract-tx
  [bin params]
  (-> params
      (assoc :data bin)
      (map->tx)))



