(ns cloth.tx
  (:require [cloth.util :as util]
            [cloth.keys :as keys]
            [cuerdas.core :as c]
            ethereumjs-tx))

(def Tx js/EthTx)

(defn map->tx [params]
  (reduce #(assoc % (c/camelize (name (key %2)))
                    (util/add0x (val %2)))
          {} params))

(defn create-tx [params]
  (Tx. (clj->js params)))

(defn encode [tx]
  (.serialize tx))

(defn hex [tx]
  (util/add0x (util/->hex (encode tx))))

(defn sign-tx [tx priv]
  (.sign tx (keys/get-private-key priv))
  tx)

(defn value-tx [params]
  (create-tx (map->tx params)))

(defn encode-fn-name [fname types]
  (-> (str (name fname) "(" (c/join "," (map name types)) ")")
      (util/sha3)
      (util/->hex)
      (.slice 0 8)))

(defn encode-args [types args]
  (apply str (map util/solidity-format types args)))

(defn encode-fn-sig [name types args]
  (util/add0x (str (encode-fn-name name types)
                   (encode-args types args))))

(defn fn-tx
  ([contract fn-abi args]
   (let [params (if (map? (last args)) (last args) {})
         args (if (map? (last args)) (pop args) args)]
     (fn-tx contract (:name fn-abi) (map :type (:inputs fn-abi)) args params)))
  ([contract name types args params]
   (-> params
       (assoc :data (encode-fn-sig name types args)
              :to contract)
       (map->tx))))

(defn create-contract-tx
  [bin params]
  (-> params
      (assoc :data bin)
      ;:to (util/add0x (util/->hex (util/zero-pad (util/int->buffer 0) 40)))

      (map->tx)))


