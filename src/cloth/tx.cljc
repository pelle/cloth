(ns cloth.tx
  (:require
    [cloth.util :as util]
    [cloth.rlp :as rlp]
    [secp256k1.core :as ecc]
    [secp256k1.formatting.der-encoding :as der]
    [cloth.bytes :as b]
    [clojure.walk :refer [keywordize-keys]]
    [cloth.keys :as keys]
    [cuerdas.core :as c]
    [cemerick.url :as url]))

(defn map->tx [params]
  (reduce #(assoc % (keyword (c/camel (name (key %2))))
                    (if (= (key %2) :function)
                      (val %2)
                      (b/add0x (val %2))))
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
  (mapv
    (fn [tx type] (b/->bytes (:type params)))
    [:nonce :gas-price :gas-limit :to :value :data]))

;(defn create [params]
;  #?(:cljs
;     (Tx. (clj->js (map->tx params))))
;  #?(:clj
;     (let [{:keys [to value nonce gas-price gas-limit data] :as tx} params]
;       (Transaction. (if nonce (b/->bytes nonce))
;                     (if gas-price (b/->bytes gas-price))
;                     (if gas-limit (b/->bytes gas-limit))
;                     (if to (b/->bytes to))
;                     (if value (b/->bytes value))
;                     (if data (b/->bytes data))))))


(defn decode [rawtx]
  (let [t (rlp/decode (b/->bytes rawtx))]
    {:nonce     (get t 0)
     :gas-price (b/->uint (get t 1))
     :gas-limit (b/->uint (get t 2))
     :to        (b/add0x (b/->hex (get t 3)))
     :value     (b/->uint (get t 4))
     :data      (b/add0x (b/->hex (get t 5)))
     :v         (b/add0x (b/->hex (get t 6)))
     :r         (b/add0x (b/->hex (get t 7)))
     :s         (b/add0x (b/->hex (get t 8)))}))

(defn- base-fields [tx]
  [(b/uint->bytes (:nonce tx 0))
   (b/uint->bytes (:gas-price tx 0))
   (b/uint->bytes (:gas-limit tx 0))
   (b/->bytes (:to tx "0x00"))
   (b/uint->bytes (:value tx 0))
   (b/->bytes (:data tx "0x00"))])

(defn encode [tx]
  (let [fields (base-fields tx)
        fields (if (:v tx)
                 (concat [(b/->bytes (:v tx))
                          (b/->bytes (:r tx))
                          (b/->bytes (:s tx))])
                 fields)]
    (b/add0x (b/->hex (rlp/encode fields)))))

(defn sender [tx]
  (let [input (b/->hex (rlp/encode (base-fields tx)))
        signature (der/DER-encode-ECDSA-signature {:recover (b/->bytes (:v tx))
                                                   :R       (b/->bytes (:r tx))
                                                   :S       (b/->bytes (:s tx))})
        public  (ecc/recover-public-key input signature)]
    (println "pub: " public)
    (keys/->address (b/add0x public))
    )
  )
(defn tx->map [tx]
  (when tx
    (let [m (decode tx)]
      (if-let [from (sender tx)]
        (assoc m :from from)
        m))))

(defn ->hex [tx]
  (-> tx
      ;(tx->b)
      (b/->hex)
      (b/add0x)))

(defn sign [tx priv]
  (let [payload (base-fields tx)
        encoded (rlp/encode payload)
        signature (der/DER-decode-ECDSA-signature (ecc/sign (b/strip0x priv) encoded))
        payload (concat payload
                        [(b/->bytes (b/add0x (:recover signature)))
                         (b/->bytes (b/add0x (:R signature)))
                         (b/->bytes (b/add0x (:S signature)))])
        _ (prn signature)
        ]
    (-> payload
        (rlp/encode)
        (b/->hex)
        (b/add0x))))

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
  ([bin params]
   (create-contract-tx bin params {:inputs [] :type "constructor"} []))
  ([bin params fn-abi args]
   (let [types (map :type (:inputs fn-abi))
         bin (if (empty? types)
               bin
               (str bin (util/encode-args types args)))]
     (-> params
         (assoc :data bin)
         (map->tx)))))



