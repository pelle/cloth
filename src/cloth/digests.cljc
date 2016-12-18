(ns cloth.digests
  (:require [cloth.bytes :as b]
    #?@(:cljs [[sha3]]))
  (:import
    #?(:clj [org.bouncycastle.jcajce.provider.digest
             SHA256$Digest RIPEMD160$Digest Keccak$Digest256]
       :cljs [goog.crypt Sha256])))

(defn do-digest [digest data]
  #?(:clj
     (let [input (b/->bytes data)]
       (.update digest input)
       (.digest digest))
     :cljs
     (do
       (.update digest (b/->bytes data))
       (b/->uint8-array (.digest digest)))))

(defn sha3 [data]
  #?(:cljs
     (js/keccak_256 data)
     :clj
     (do-digest (Keccak$Digest256.) data)))

(defn sha256 [data]
  #?(:cljs
     (do-digest (Sha256.) data)
     :clj
     (do-digest (SHA256$Digest.) data)))


;(defn ripemd160 [data]
;  #?(:cljs null
;     :clj
;     (do-digest (RIPEMD160$Digest.) data)))
