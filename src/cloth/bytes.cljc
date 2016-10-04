(ns cloth.bytes
  (:require   [cuerdas.core :as c]
    #?@(:cljs [[goog.crypt]
               [goog.math.Integer]
               [bn]]))
  #?(:clj
     (:import
       [org.spongycastle.util.encoders Hex]
       [org.spongycastle.util BigIntegers])))
#?(:cljs
   (def BN js/BN))

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

(defn negative-int? [string]
  (and (string? string)
       (re-find #"^(0x)?[8-f]" string)))

(defn prefixed-hex? [string]
  (and (string? string)
       (re-find #"^(0x)[0-9a-fA-F]*$" string)))

#?(:cljs
   (defn ->uint8-array [array]
     (.from js/Uint8Array array)))

(defn bytes? [a]
  #?(:cljs (= (aget a "constructor" "name")
              "Uint8Array")
     :clj (clojure.core/bytes? a)))

(defn byte-array [length]
  #?(:cljs (js/Uint8Array. length)
     :clj (clojure.core/byte-array length)))

(defn even-hex->bytes [data]
  #?(:cljs (->uint8-array (goog.crypt/hexToByteArray data))
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
       :cljs (->uint8-array (goog.crypt/stringToUtf8ByteArray val)))
    (number? val)
    #?(:cljs (goog.math.Integer/fromNumber val)
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
       :cljs (->uint8-array (goog.crypt/stringToUtf8ByteArray val)))
    (number? val)
    #?(:cljs (goog.math.Integer/fromNumber val)
       :clj  (BigIntegers/asUnsignedByteArray (biginteger val)))))

(defn uint->bytes
  "converts anything into platform native byte array. Non 0x prefixed hex strings are interpreted as strings and as such are not hex decoded"
  [val]
  (let [val (if (nil? val) 0 val)]
    #?(:cljs (goog.math.Integer/fromNumber val)
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

(defn hex0x [ba]
  (add0x (->hex ba)))

(defn zeros
  "Returns a ba or byte-array filled with 0s"
  [length]
  #?(:cljs
     (js/Uint8Array. length))
  #?(:clj (byte-array length)))

#?(:cljs
   (defn clone-and-pad-byte-array
     ([ba l] (clone-and-pad-byte-array ba l true))
     ([ba l left-pad?]
      (if (< l (alength ba))
        (.slice ba (- (alength ba) l))
        (let [padded (js/Uint8Array. l)
              offset (if left-pad? (- l (alength ba)) 0)]
          (doall
            (for [i (range l)]
              (aset padded (+ offset i) (aget ba i))))
          padded)))))

(defn pad
  "Left Pads an `Array` or `Buffer` with leading zeros until it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [ba l]
  #?(:cljs (clone-and-pad-byte-array ba l))
  #?(:clj
     (if (= (count ba) l)
       ba
       (let [padded (byte-array l)]
         (if (> l (count ba))
           (java.lang.System/arraycopy ba 0 padded (- l (count ba)) (count ba))
           (java.lang.System/arraycopy ba (- (count ba) l) padded 0 l))
         padded))))

;; TODO figure out what the difference is supposed to be between this and pad
(defn negative-pad
  "Left Pads an `Array` or `Buffer` with leading zeros until it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [ba l]
  #?(:cljs (clone-and-pad-byte-array ba l))
  #?(:clj
     (if (= (count ba) l)
       ba
       (let [padded (byte-array l (repeat 255))]
         (if (> l (count ba))
           (java.lang.System/arraycopy ba 0 padded (- l (count ba)) (count ba))
           (java.lang.System/arraycopy ba (- (count ba) l) padded 0 l))
         padded))))

(defn pad-single-byte [ba]
  (pad ba (inc (alength ba))))

(defn rpad
  "Right Pads an `Array` or `Buffer` with trailing zeros until it has `length` bytes.
   Or it truncates the beginning if it exceeds."
  [ba l]
  #?(:cljs (clone-and-pad-byte-array ba l false))
  #?(:clj
     (if (= (count ba) l)
       ba
       (let [padded (byte-array l)]
         (if (> l (count ba))
           (java.lang.System/arraycopy ba 0 padded 0 (count ba))
           (java.lang.System/arraycopy ba (- (count ba) l) padded 0 l))
         padded))))

(defn hex->int [hex]
  #?(:cljs (if (negative-int? hex)
             (let [stripped (strip0x hex)]
               (.fromTwos (BN. stripped 16) (* 4 (count stripped))))
             (BN. (strip0x hex) 16))
     :clj  (BigInteger. hex 16)))

(defn hex->uint [hex]
  #?(:cljs (BN. (strip0x hex) 16)
     :clj  (BigInteger. hex 16)))

(defn- ->big-integer [ba]
  #?(:cljs (BN. ba)
     :clj  (biginteger ba)))

(defn ->int [data]
  (if data
    (cond
      (number? data)
        (->big-integer data)
      (hex? data)
        (hex->int data)
      :else
      (->big-integer (->bytes data)))
    0))

(defn ->uint [data]
  (if data
    (cond
      (number? data)
        (->big-integer data)
      (hex? data)
        (hex->uint data)
      :else
      (-> data
          ->bytes
          pad-single-byte
          ->big-integer))
    0))

(defonce zero (->big-integer 0))

(defn int->hex [i]
  (let [bi (->big-integer i)]
    (if (.eq bi zero)
      ""
      (pad-to-even (.toString bi 16)))))
