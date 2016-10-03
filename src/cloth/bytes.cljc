(ns cloth.bytes
  (:require [cuerdas.core :as c]
    #?@(:cljs [[goog.crypt]
               [goog.math.Integer :as Integer]]))
  #?(:clj
     (:import
       [org.spongycastle.util.encoders Hex]
       [org.spongycastle.util BigIntegers])))


(defn add0x [input]
  (if (string? input)
    (if (re-find #"^0x" input)
      input
      (str "0x" input))
    input))

(defn strip0x [input]
  (if (and (string? input) (re-find #"^0x" input))
    (c/slice input 2)
    input))

(defn pad-to-even [hex]
  (if (= (mod (count hex) 2) 1)
    (str "0" hex)
    hex))

(defn hex? [string]
  (and (string? string)
       (re-find #"^(0x)?[0-9a-fA-F]*$" string)))

(defn prefixed-hex? [string]
  (and (string? string)
       (re-find #"^(0x)[0-9a-fA-F]*$" string)))

(defn bytes? [a]
  #?(:cljs (= (aget (.from BA "hello") "constructor" "name")
              "Uint8Array")
     :clj (clojure.core/bytes? a)))

(defn even-hex->bytes [data]
  #?(:cljs (goog.crypt/hexToByteArray data)
     :clj  (Hex/decode data)))

(defn hex->bytes
  "converts a hex into platform native byte array"
  [data]
  (-> data
      strip0x
      pad-to-even
      even-hex->bytes))

(defn ->bytes
  "converts anything into platform native byte array. Any string that looks like a hex is converted into bytes as if it was hex"
  [val]
  (cond
    (bytes? val) val
    (hex? val) (hex->bytes val)
    (string? val)
    #?(:clj  (.getBytes val)
       :cljs (goog.crypt/stringToUtf8ByteArray val))
    (number? val)
    #?(:cljs (Integer/from val)
       :clj  (BigIntegers/asUnsignedByteArray (biginteger val)))))

(defn strict->bytes
  "converts anything into platform native byte array. Non 0x prefixed hex strings are interpreted as strings and as such are not hex decoded"
  [val]
  (cond
    (bytes? val) val
    (prefixed-hex? val) (hex->bytes val)
    (string? val)
    #?(:clj  (.getBytes val)
       :cljs (goog.crypt/stringToUtf8ByteArray val))
    (number? val)
    #?(:cljs (Integer/from val)
       :clj  (BigIntegers/asUnsignedByteArray (biginteger val)))))

(defn ->hex
  "Convert anything into a hex encoded string"
  [data]
  (if (hex? data)
    (strip0x data)
    (let [data (->bytes data)]
      #?(:cljs (goog.crypt/byteArrayToHex data)
         :clj (Hex/toHexString data)))))

(defn hex0x [buffer]
  (add0x (->hex buffer)))

(defn zeros
  "Returns a buffer or byte-array filled with 0s"
  [length]
  #?(:cljs
     (js/Uint8Array. length))
  #?(:clj (byte-array length)))

(defn pad
  "Left Pads an `Array` or `Buffer` with leading zeros until it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [buffer l]
  #?(:cljs ((aget eth-util "setLengthLeft") buffer l))
  #?(:clj
     (if (= (count buffer) l)
       buffer
       (let [padded (byte-array l)]
         (if (> l (count buffer))
           (java.lang.System/arraycopy buffer 0 padded (- l (count buffer)) (count buffer))
           (java.lang.System/arraycopy buffer (- (count buffer) l) padded 0 l))
         padded))))

(defn negative-pad
  "Left Pads an `Array` or `Buffer` with leading zeros until it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [buffer l]
  #?(:cljs ((aget eth-util "setLengthLeft") buffer l))
  #?(:clj
     (if (= (count buffer) l)
       buffer
       (let [padded (byte-array l (repeat 255))]
         (if (> l (count buffer))
           (java.lang.System/arraycopy buffer 0 padded (- l (count buffer)) (count buffer))
           (java.lang.System/arraycopy buffer (- (count buffer) l) padded 0 l))
         padded))))

(def lpad pad)

(defn pad-single-byte [ba]
  (pad ba (inc (alength ba))))

(defn rpad
  "Right Pads an `Array` or `Buffer` with trailing zeros until it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [buffer l]
  #?(:cljs
     (if (< l (.-length buffer))
       (.slice buffer (- (.-length buffer) l))
       ((aget eth-util "setLengthRight") buffer l)))
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

(defn- ->big-integer [ba]
  #?(:cljs (Integer/from ba)
     :clj  (biginteger ba)))

(defn ->int [data]
  (if (number? data)
    (->big-integer data)
    (->big-integer (->bytes data))))

(defn ->uint [data]
  (if (number? data)
    (->big-integer data)
    (-> data
        ->bytes
        pad-single-byte
        ->big-integer)))

(defn int->hex [i]
  (->hex i))

(comment
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
         ((aget eth-util "toUnsigned") (biginteger number)))
      #?(:clj (bn/->b (biginteger number)))))

  (defn int->hex
    "Converts a `Number` into a hex `String`"
    [number]
    (b/->hex (intb/->bytes number)))

  (defn b->int
    "Interprets a `Buffer` as a signed integer and returns a `BN` or 'BigInteger. Assumes 256-bit numbers."
    [b]
    #?(:cljs
       ((aget eth-util "fromSigned") b))
    #?(:clj (if (and b (not= (count b) 0))
              (BigInteger. b)
              0)))

  (defn b->uint
    "Interprets a `Buffer` as a unsigned integer and returns a `BN`. Assumes 256-bit numbers."
    [b]
    #?(:cljs
       ((aget eth-util "bufferToInt") b))
    #?(:clj (if b (BigInteger. b) 0)))

  (defn ->uint
    "Convers a hex `string` into a uint"
    [string]
    (-> (strip0x string)
        ((partial str "00"))
        (b/->bytes)
        (b->uint)))

  (defn spy [x]
    (prn x)
    x)

  (defn ->int
    "Convers a hex `string` into a signed integer"
    [hex]
    #?(:cljs
       (-> (->bytes hex)
           (b->int)))
    #?(:clj
       (-> (->bytes hex)
           (b->int)))))

