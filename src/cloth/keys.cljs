(ns cloth.keys
  (:require [cloth.util :as util]))

(def secp256k1 (aget util/eth-util "secp256k1"))

(defn random-bytes
  ([] (random-bytes 32))
  ([length]
   (util/Buffer. (. js/window.crypto getRandomValues (js/Uint8Array. length)))))

(defn create-private-key []
  (loop []
    (let [key (random-bytes)]
      (if-not ((aget secp256k1 "privateKeyVerify") key)
        (recur)
        key))))

(defn ->public-key [private-key]
  ((aget util/eth-util "privateToPublic") private-key))


(defn ->address [public-key]
  (util/hex0x ((aget util/eth-util "pubToAddress") public-key)))

(defn keypair
  ([private-key] (keypair private-key (->public-key private-key)))
  ([private-key public-key]
   {:private-key (util/hex0x private-key)
    :public-key  (util/hex0x public-key)
    :address     (->address public-key)}))

(defn get-private-key
  "pass a keypair map or a private-key either hex or buffer"
  [kp-or-private-key]
  (if (map? kp-or-private-key)
    (util/hex-> (:private-key kp-or-private-key))
    (if (string? kp-or-private-key)
      (util/hex-> kp-or-private-key)
      kp-or-private-key)))

(defn create-keypair
  "Creates a map of hex encoded keypair with
   :private-key, :public-key and :address keys"
  []
  (keypair (create-private-key)))
