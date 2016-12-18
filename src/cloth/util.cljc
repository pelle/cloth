(ns cloth.util
  (:require [cuerdas.core :as c]
            [cloth.bytes :as b :refer [->hex ->bytes pad rpad add0x strip0x]]
    #?@(:cljs [[ethereumjs-tx]
               [goog.crypt]
               [sha3]]))
  #?(:clj
     (:import
       [org.ethereum.crypto HashUtil])))

#?(:cljs
   (def eth-js js/EthJS))

#?(:cljs
   (def eth-util (aget eth-js "Util")))

#?(:cljs
   (def Buffer (aget eth-js "Buffer" "Buffer")))
;
;#?(:cljs
;   (def BN (aget eth-js "BN")))
;
;#?(:cljs
;   (defn biginteger [buffer]
;     (BN. (or buffer 0))))
;
;#?(:cljs
;   (defn b/->bytesuffer [val]
;     ((aget eth-util "toBuffer") (clj->js val))))

(defn sha3 [data]
  #?(:cljs
     (js/keccak_256 data))
  #?(:clj
     (HashUtil/sha3 (b/->bytes data))))

(defn sha256 [data]
  #?(:cljs
     ((aget eth-util "sha256") data))
  #?(:clj (HashUtil/sha256 (b/->bytes data))))

;(defn ripemd160 [data]
;  #?(:cljs
;     ((aget eth-util "ripemd160") data))
;  #?(:clj
;     (HashUtil/ripemd160 (b/->bytes data))))

#?(:cljs
   (defn generate-contract-address
     [from nonce]
     ((aget eth-util "generateAddress") from nonce)))


(defonce solidity-true
         "0000000000000000000000000000000000000000000000000000000000000001")
(defonce solidity-false
         "0000000000000000000000000000000000000000000000000000000000000000")

(defn solidity-uint [length val]
  (->hex (pad (b/uint->bytes val) (/ length 8))))

(defn solidity-int [length val]
  (let [pad (if (neg? val) b/negative-pad pad)]
    (->hex (pad (b/int->bytes val) (/ length 8)))))

(defn extract-type [type _]
  (let [ts (name type)]
    (cond
      (re-find #"\[\]$" ts) :dynamic-array
      (re-find #"\[\d+\]$" ts) :fixed-array
      :else
      (keyword (re-find #"^[A-z]+" ts)))))

(defn storage-length [length]
  (let [multiples (int (/ length 32))
        multiples (if (= length (* 32 multiples))
                    multiples
                    (inc multiples))]
    (* 32 multiples)))

(defn parse-int [s]
  #?(:cljs (js/parseInt s))
  #?(:clj (Integer. s)))

(defmulti extract-size keyword)

(defmethod extract-size :default [type]
  (when-let [size (re-find #"\d+" (name type))]
    (storage-length (parse-int size))))

(defmethod extract-size :address [type]
  32)

(defn dynamic-type? [type]
  (re-find #"^(bytes|string|.*\[\])$" (name type)))

(defmulti decode-solidity extract-type)

(defmethod decode-solidity :default
  [t v]
  (println "decoding unsupported solidity return value " t "= " v)
  v)

(defmethod decode-solidity :fixed-array
  [type values]
  ;; This will only work at the moment where all the elements of the array are the same size
  (let [[_ type array-length] (re-find #"(.*)\[(\d+)\]" type)
        size (extract-size type)
        pattern (re-pattern (str ".{" (* 2 size) "}"))]
    (mapv (partial decode-solidity type) (re-seq pattern values))))

(defmethod decode-solidity :dynamic-array
  [type value]
  ;; This will only work at the moment where all the elements of the array are the same size
  (let [[_ type] (re-find #"(.*)\[]" type)
        type (keyword type)
        array-length (decode-solidity :uint (c/slice value 0 64))
        data (c/slice value 64)
        size (extract-size type)
        pattern (re-pattern (str ".{" (* 2 size) "}"))]
    (mapv (partial decode-solidity type) (re-seq pattern data))))

(defmethod decode-solidity :address
  [_ v]
  (add0x (c/slice v -40)))

(defmethod decode-solidity :uint
  [_ v]
  (b/->uint v))

(defmethod decode-solidity :int
  [_ v]
  (b/->int v))

#?(:clj
   (defmethod decode-solidity :fixed
     [type v]
     (let [n (or (extract-size type) 128)]
       (/ (bigdec (b/->int v)) (.pow (biginteger 2N) n)))))

#?(:clj
   (defmethod decode-solidity :ufixed
     [type v]
     (let [n (or (extract-size type) 128)]
       (/ (bigdec (b/->int v)) (.pow (biginteger 2N) n)))))


(defmethod decode-solidity :bytes
  [type v]
  (if-let [size (extract-size type)]
    (->bytes v)
    (let [size (b/->int (c/slice v 0 64))]
      (->bytes (c/slice v 64 (+ 64 (* 2 size)))))))

(defmethod decode-solidity :string
  [_ v]
  (b/->str (decode-solidity :bytes v)))

(defmethod decode-solidity :bool
  [_ v]
  (= "0000000000000000000000000000000000000000000000000000000000000001" v))

;; https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI
;; - ints
;; - fixed and ufixed
(defmulti encode-solidity extract-type)
(defmethod encode-solidity :bool
  [_ val] (if val solidity-true solidity-false))

(defmethod encode-solidity :fixed-array
  [type values]
  (let [[_ type size] (re-find #"(.*)\[(\d+)\]" type)]
    (apply str (map (partial encode-solidity type) values))))

(defmethod encode-solidity :dynamic-array
  [type values]
  (let [[_ type] (re-find #"(.*)\[]" type)]
    (apply (partial str (solidity-uint 256 (count values))) (map (partial encode-solidity type) values))))

(defn round-to-multiple-of [v m]
  (if (= (mod v m) 0)
    v
    (* m (inc (int (/ v m))))))

(defmethod encode-solidity :uint
  [type val]
  (solidity-uint (round-to-multiple-of (extract-size type) 256) val))

(defmethod encode-solidity :int
  [type val]
  (solidity-int (round-to-multiple-of (extract-size type) 256) val))

#?(:clj
   (defmethod encode-solidity :fixed
     [type v]
     ;; TODO currently supports fixedMxN where M is 128
     (let [n (or (extract-size type) 128)]
       (solidity-int 256 (biginteger (* (bigdec v) (.pow (biginteger 2N) n)))))))

#?(:clj
   (defmethod encode-solidity :ufixed
     [type v]
     ;; TODO currently supports ufixedMxN where M is 128
     (let [n (or (extract-size type) 128)]
       (solidity-uint 256 (biginteger (* (bigdec v) (.pow (biginteger 2N) n)))))))

(defmethod encode-solidity :address
  [_ val]
  (-> (strip0x val)
      (->bytes)
      (pad 32)
      (->hex)))

(defmethod encode-solidity :bytes
  [type val]
  #?(:cljs
     (let [buffer (b/strict->bytes val)]
       (if-let [size (extract-size type)]
         (->hex (b/rpad buffer size))
         (let [size (.-length buffer)]
           (str (solidity-uint 256 size)
                (->hex (rpad buffer (storage-length size))))))))
  #?(:clj
     (let [buffer (b/strict->bytes val)]
       (if-let [size (extract-size type)]
         (->hex (rpad buffer size))
         (let [size (count buffer)]
           (str (solidity-uint 256 size)
                (->hex (rpad buffer (storage-length size)))))))))

(defmethod encode-solidity :string
  [_ val]
  (encode-solidity :bytes val))

(defn encode-solidity-call-sig [fname types]
  (-> (str (name fname) "(" (c/join "," (map name types)) ")")
      (sha3)
      (->hex)))

(defn encode-event-sig [fname types]
  (add0x (encode-solidity-call-sig fname types)))

(defn encode-fn-name [fname types]
  (-> (encode-solidity-call-sig fname types)
      (c/slice 0 8)))

(defn encode-args [types args]
  (let [{:keys [head tail]}
        (reduce
          (fn [c [t v]]
            (let [encoded (encode-solidity t v)
                  ;;; This assumes all head items are 32 bytes long which is wrong
                  head-length (* 32 (count types))]
              (if (dynamic-type? t)
                {:count (+ (:count c) 32)
                 :head  (str (:head c)
                             (solidity-uint 256 (+ head-length
                                                   (/ (count (:tail c)) 2))))
                 :tail  (str (:tail c) encoded)}
                {:count (+ (:count c) (count encoded))
                 :head (str (:head c) encoded)
                 :tail (:tail c)})))
          {:count 0 :head "" :tail ""} (map vector types args))]
    (str head tail)))

(defn encode-fn-sig
  "Encode function for use in a data field"
  [fname types args]
  (add0x (str (encode-fn-name fname types)
                   (encode-args types args))))

(defmulti prn-solidity-value (fn [type _] type))
(defmethod prn-solidity-value :default
  [_ val]
  (if (vector? val)
    (str "[" (c/join "," (map str val)) "]")
    val))
(defmethod prn-solidity-value :string
  [_ val] (str "\"" val "\""))
(defmethod prn-solidity-value :bytes
  [_ val]
  (if (string? val)
    (if (re-find #"^0x[0-9a-f]*$" val)
      val
      (prn-solidity-value :string val))
    (->hex val)))


(defn encode-fn-param
  "Encode function for use in ethereum uri function param"
  [fname types args]
  (str fname "(" (c/join "," (map #(str (name %) " " (prn-solidity-value % %2)) types args)) ")"))

(defn decode-solidity-data [types data]
  (if (or (empty? data) (empty? types))
    nil
    (let [type (first types)
          data-size (round-to-multiple-of (or (extract-size type) 256) 256)
          data-size (if (dynamic-type? type)
                      (+ data-size 256)
                      data-size)
          data-size (/ data-size 4)
          start (if (dynamic-type? type)
                  64 0)
          chunk (c/slice data start (+ start data-size))
          ]
      ;(prn {:type type :data-size data-size :start start :chunk chunk})
      (cons (decode-solidity type chunk)
            (lazy-seq (decode-solidity-data (rest types) (c/slice data data-size)))))))

(defn decode-return-value
  ([output-abi data]
    (decode-return-value output-abi data true))
  ([output-abi data single-item-if-possible?]
   (let [outputs (decode-solidity-data (map :type output-abi) (strip0x data))
         outputs (if (and (< (count outputs) 2)
                          single-item-if-possible?)
                   (first outputs)
                   (apply merge
                          (map
                            (fn [n v] {(keyword (c/kebab (name n))) v})
                            (map :name output-abi)
                            outputs)))]
     outputs)))

