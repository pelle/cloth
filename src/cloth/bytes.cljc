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
      (if (= input "") "0x00" (str "0x" input)))
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
    (nil? val) val
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
    (nil? val) val
    (bytes? val) val
    (prefixed-hex? val) (hex->bytes val)
    (string? val)
    #?(:clj  (.getBytes val)
       :cljs (goog.crypt/stringToUtf8ByteArray val))
    (number? val)
    #?(:cljs (Integer/from val)
       :clj  (BigIntegers/asUnsignedByteArray (biginteger val)))))

(defn uint->bytes
  "converts anything into platform native byte array. Non 0x prefixed hex strings are interpreted as strings and as such are not hex decoded"
  [val]
  (let [val (if (nil? val) 0 val)]
    #?(:cljs (Integer/from val)
       :clj  (BigIntegers/asUnsignedByteArray (biginteger val)))))

(defn ->hex
  "Convert anything into a hex encoded string"
  [data]
  (when data
    (if (hex? data)
      (strip0x data)
      (let [data (->bytes data)]
        #?(:cljs (goog.crypt/byteArrayToHex data)
           :clj  (Hex/toHexString data))))))

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
  (if data
    (if (number? data)
      (->big-integer data)
      (->big-integer (->bytes data)))
    0))

(defn ->uint [data]
  (if data
    (if (number? data)
      (->big-integer data)
      (-> data
          ->bytes
          pad-single-byte
          ->big-integer))
    0))

(defn int->hex [i]
  (->hex i))
