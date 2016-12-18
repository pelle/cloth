(ns cloth.keys
  (:require
    [secp256k1.core :as ecc]
    [cuerdas.core :as c]
    [cloth.util :as util]
    [cloth.digests :as d]
    [cloth.bytes :as b]))

#?(:cljs
   (defn random-bytes
     ([] (random-bytes 32))
     ([length]
      (. js/window.crypto getRandomValues (js/Uint8Array. length)))))

(defn- encode-pub-key [pub]
  (ecc/x962-encode pub :compressed false))

(defn ->public-key [private-key]
   (-> private-key
       (b/->hex)
       (ecc/private-key)
       (ecc/public-key)
       (ecc/x962-encode :compressed false)
      ))

(defn ->address [kp-or-public-key]
  (let [public-key (or (:public-key kp-or-public-key)
                       (if (:private-key kp-or-public-key)
                         (->public-key (:private-key kp-or-public-key))
                         kp-or-public-key))]
    (b/add0x (c/slice (b/->hex (d/sha3 (b/->bytes (c/slice (b/strip0x public-key) 2)))) 24))))

(defn keypair
  [private-key]
  (let [public-key (b/add0x (->public-key (b/strip0x private-key)))]
    {:private-key (b/add0x private-key)
     :address     (->address public-key)}))

(defn get-private-key
  "pass a keypair map or a private-key either hex or buffer and returns a private key for signing pupr"
  [kp-or-private-key]
  (:private-key kp-or-private-key kp-or-private-key))

(defn create-keypair
  "Creates a map of hex encoded keypair with
   :private-key and :address keys"
  []
  (let [ap (ecc/generate-address-pair)]
    (keypair (:private-key ap))))
