(ns cloth.util
  (:require [cuerdas.core :as cuerdas]
    #?@(:cljs [ethereumjs-tx]))
  #?(:clj
     (:import
       [org.spongycastle.util.encoders Hex]
       [org.spongycastle.util BigIntegers]
       [org.ethereum.crypto HashUtil])))

#?(:cljs
   (def eth-util js/ethUtil))
#?(:cljs
   (def Buffer js/Buffer))
#?(:cljs
   (defn ->buffer [val]
     ((aget eth-util "toBuffer") (clj->js val))))

#?(:cljs
   (defn ->hex
     "Convert a buffer into a hex encoded string"
     [buffer]
     (when buffer
       (.toString buffer "hex"))))

#?(:clj
   (defn ->hex
     "Convert a byte array into a hex encoded string"
     [ba]
     (when ba
       (let [ba (if (empty? ba)
                  (byte-array 1)
                  ba)]
         (Hex/toHexString ba)))))


(defn add0x [input]
  (if (string? input)
    (if (re-find #"^0x" input)
      input
      (str "0x" input))
    input))

(defn strip0x [input]
  (if (re-find #"^0x" input)
    (cuerdas/slice input 2)
    input))

(defn pad-to-even [hex]
  (if (= (mod (count hex) 2) 1)
    (str "0" hex)
    hex))

#?(:cljs
   (defn hex->b [hex]
     (Buffer. hex "hex")))

#?(:clj
   (defn hex->b [hex]
     (Hex/decode hex)))

(defn hex->
  "Converts a hex string to buffer or bytearray"
  [hex]
  (when hex
    (-> (strip0x hex)
        (pad-to-even)
        (hex->b))))

(defn hex0x [buffer]
  (add0x (->hex buffer)))

#?(:clj
   (defn ->b [b]
     (if (string? b)
       (.getBytes b)
       b)))

(defn sha3 [data]
  #?(:cljs
     ((aget eth-util "sha3") data))
  #?(:clj
     (HashUtil/sha3 (->b data))))

(defn sha256 [data]
  #?(:cljs
     ((aget eth-util "sha256") data))
  #?(:clj (HashUtil/sha256 (->b data))))

(defn ripemd160 [data]
  #?(:cljs
     ((aget eth-util "ripemd160") data))
  #?(:clj
     (HashUtil/ripemd160 (->b data))))

(comment (def rlp (aget eth-util "rlp"))

         (defn rlp-encode [data]
           ((aget rlp "encode") (clj->js data)))

         (defn rlp-decode [data]
           (js->clj ((aget rlp "decode") data))))
#?(:cljs
   (defn generate-contract-address
     [from nonce]
     ((aget eth-util "generateAddress") from nonce)))

(defn zeros
  "Returns a buffer or byte-array filled with 0s"
  [length]
  #?(:cljs
     ((aget eth-util "zeros") length))
  #?(:clj (byte-array length)))

(defn pad
  "Left Pads an `Array` or `Buffer` with leading zeros until it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [buffer l]
  #?(:cljs ((aget eth-util "pad") buffer l))
  #?(:clj
     (if (= (count buffer) l)
       buffer
       (let [padded (byte-array l)]
         (if (> l (count buffer))
           (java.lang.System/arraycopy buffer 0 padded (- l (count buffer)) (count buffer))
           (java.lang.System/arraycopy buffer (- (count buffer) l) padded 0 l))
         padded))))

(def lpad pad)

(defn rpad
  "Right Pads an `Array` or `Buffer` with trailing zeros untill it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [buffer l]
  #?(:cljs ((aget eth-util "rpad") buffer l))
  #?(:clj
     (if (= (count buffer) l)
       buffer
       (let [padded (byte-array l)]
         (if (> l (count buffer))
           (java.lang.System/arraycopy buffer 0 padded 0 (count buffer))
           (java.lang.System/arraycopy buffer (- (count buffer) l) padded 0 l))
         padded))))

#?(:cljs
   (defn unpad
     "Trims leading zeros from a `Buffer` or an `Array`"
     [buffer]
     ((aget eth-util "unpad") buffer)))

(defn b->bn
  "Interprets a `Buffer` as a signed integer and returns a `BN`. Assumes 256-bit numbers."
  [b]
  #?(:cljs
     ((aget eth-util "fromSigned") b))
  #?(:clj (if b (BigInteger. 1 b) 0)))

(defn bn->b
  "Converts a `BN` or `BigNumber` to an unsigned integer and returns it as a `Buffer` or ByteArray. Assumes 256-bit numbers."
  [number]
  #?(:cljs
     ((aget eth-util "toUnsigned") number))
  #?(:clj (BigIntegers/asUnsignedByteArray number)))

(defn int->b
  "Converts an `Number` to a `Buffer`"
  [number]
  (let [number (if (nil? number) 0 number)]
    #?(:cljs
       ((aget eth-util "intToBuffer") number))
    #?(:clj (bn->b (biginteger number)))))

(defn int->hex
  "Converts a `Number` into a hex `String`"
  [number]
  (->hex (int->b number)))

(defn b->int
  "Converts a `Buffer` or `bytearray` to a `Number`"
  [b]
  #?(:cljs ((aget eth-util "bufferToInt") b))
  #?(:clj (b->bn b)))

(defn hex->int
  "Convers a hex `string` into a number"
  [string]
  (b->int (hex-> string)))

(defonce solidity-true
         "0000000000000000000000000000000000000000000000000000000000000001")
(defonce solidity-false
         "0000000000000000000000000000000000000000000000000000000000000000")

(defn solidity-uint [length val]
  (->hex (pad (int->b val) length)))

(defn extract-type [type _]
  (keyword (re-find #"^[A-z]+" (name type))))

(defn storage-length [length]
  (let [multiples (int (/ length 32))
        multiples (if (= length (* 32 multiples))
                    multiples
                    (inc multiples))]
    (* 32 multiples)))

(defn parse-int [s]
  #?(:cljs (js/parseInt s))
  #?(:clj (Integer. s)))

(defn extract-size [type]
  (when-let [size (re-find #"\d+" (name type))]
    (storage-length (parse-int size))))

;; https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI
;; TODO still figure out how to do dynamic types correctly
;; - ints
;; - fixed and ufixed
(defmulti solidity-format extract-type)
(defmethod solidity-format :bool
  [_ val] (if val solidity-true solidity-false))
(defmethod solidity-format :uint
  [type val]
  (solidity-uint (or (extract-size type) 256) val))
(defmethod solidity-format :address
  [_ val]
  (strip0x val))

(defmethod solidity-format :bytes
  [type val]
  #?(:cljs
     (let [buffer (Buffer. val)]
       (if-let [size (extract-size type)]
         (->hex (rpad buffer size))
         (let [size (.-length buffer)]
           (str (solidity-uint 32 size)
                (->hex (rpad buffer (storage-length size))))))))
  #?(:clj
     (let [buffer (.getBytes val)]
       (if-let [size (extract-size type)]
         (->hex (rpad buffer size))
         (let [size (count buffer)]
           (str (solidity-uint 32 size)
                (->hex (rpad buffer (storage-length size)))))))))

(defmethod solidity-format :string
  [_ val]
  (solidity-format :bytes val))
