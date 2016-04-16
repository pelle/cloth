(ns cloth.util
  (:require ethereumjs-tx))
;; Bindings to https://github.com/ethereumjs/ethereumjs-util/blob/master/docs/index.md

(def eth-util js/ethUtil)
(def Buffer js/Buffer)

(defn ->buffer [val]
  ((aget eth-util "toBuffer") (clj->js val)))

(defn ->hex [buffer]
  (.toString buffer "hex"))

(defn add0x [input]
  (if (string? input)
    (if (re-find #"^0x" input)
      input
      (str "0x" input))
    input))

(defn strip0x [input]
  (if (re-find #"^0x" input)
    (.slice input 2)
    input))

(defn pad-to-even [hex]
  ((aget eth-util "padToEven") hex))

(defn hex-> [hex]
  (-> (strip0x hex)
      (pad-to-even)
      (Buffer. "hex")))

(defn hex0x [buffer]
  (add0x (->hex buffer)))

(defn sha3 [data]
  ((aget eth-util "sha3") data))

(defn sha256 [data]
  ((aget eth-util "sha256") data))

(defn ripemd160 [data]
  ((aget eth-util "ripemd160") data))

(def rlp (aget eth-util "rlp"))

(defn rlp-encode [data]
  ((aget rlp "encode") (clj->js data)))

(defn rlp-decode [data]
  (js->clj ((aget rlp "decode") data)))

(defn generate-contract-address
  [from nonce]
  ((aget eth-util "generateAddress") from nonce))

(defn zeros
  "Returns a buffer filled with 0s"
  [length]
  ((aget eth-util "zeros") length))

(defn pad
  "Left Pads an `Array` or `Buffer` with leading zeros untill it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  ([buffer length]
   ((aget eth-util "pad") buffer length)))

(defn rpad
  "Right Pads an `Array` or `Buffer` with trailing zeros untill it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  ([buffer length]
   ((aget eth-util "rpad") buffer length)))

(defn unpad
  "Trims leading zeros from a `Buffer` or an `Array`"
  [buffer]
  ((aget eth-util "unpad") buffer))

(defn int->hex
  "Converts a `Number` into a hex `String`"
  [number]
  ((aget eth-util "intToHex") number))

(defn int->buffer
  "Converts an `Number` to a `Buffer`"
  [number]
  ((aget eth-util "intToBuffer") number))

(defn buffer->int
  "Converts a `Buffer` to a `Number`"
  [number]
  ((aget eth-util "bufferToInt") number))

(defn hex->int
  "Convers a hex `string` into a number"
  [string]
  (buffer->int (hex-> string)))

(defn buffer->bn
  "Interprets a `Buffer` as a signed integer and returns a `BN`. Assumes 256-bit numbers."
  [number]
  ((aget eth-util "fromSigned") number))

(defn bn->buffer
  "Converts a `BN` to an unsigned integer and returns it as a `Buffer`. Assumes 256-bit numbers."
  [number]
  ((aget eth-util "toUnsigned") number))

(defonce solidity-true
         "0000000000000000000000000000000000000000000000000000000000000001")
(defonce solidity-false
         "0000000000000000000000000000000000000000000000000000000000000000")

(defn solidity-uint [length val]
  (->hex (pad (int->buffer val) length)))

(defn extract-type [type _]
  (keyword (re-find #"^[A-z]+" (name type))))

(defn storage-length [length]
  (let [multiples (int (/ length 32))
        multiples (if (= length (* 32 multiples))
                    multiples
                    (inc multiples))]
    (* 32 multiples)))

(defn extract-size [type]
  (when-let [size (re-find #"\d+" (name type))]
    (storage-length (js/parseInt size))))

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
  (let [buffer (Buffer. val)]
    (if-let [size (extract-size type)]
      (->hex (rpad buffer size))
      (let [size (.-length buffer)]
        (str (solidity-uint 32 size)
             (->hex (rpad buffer (storage-length size))))))))
(defmethod solidity-format :string
  [_ val]
  (solidity-format :bytes val))
