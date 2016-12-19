(ns cloth.rlp-test
  (:require [cloth.rlp :as rlp]
            [cloth.bytes :as b :refer [->bytes ->hex ->str]]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
        :clj  [
            [clojure.test :refer [is are deftest testing use-fixtures]]])))

(defn normalized [data]
  (if (b/bytes? data)
    (->hex data)
    (mapv normalized data)))


(deftest buffer-conversion
  (is (= (->hex (rlp/->ba (rlp/->buf (->bytes "ab10")))) "ab10")))

(deftest encode-tests
  (is (= (->hex (rlp/encode (->bytes "dog"))) "83646f67"))
  (is (= (->hex (rlp/encode [(->bytes "cat") (->bytes "dog")])) "c88363617483646f67"))
  (is (= (->hex (rlp/encode (->bytes ""))) "80"))
  (is (= (->hex (rlp/encode [])) "c0"))
  (is (= (->hex (rlp/encode (->bytes "0x0f"))) "0f"))
  (is (= (->hex (rlp/encode (->bytes "0x0400"))) "820400"))
  (is (= (->hex (rlp/encode [[], [[]], [[], [[]]]])) "c7c0c1c0c3c0c1c0"))
  (is (= (->hex (rlp/encode (->bytes "Lorem ipsum dolor sit amet, consectetur adipisicing elit")))
         "b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974")))

(deftest decode-tests
  (is (= (->str (rlp/decode (->bytes "0x83646f67"))) "dog"))
  (is (= (mapv ->str  (rlp/decode (->bytes "c88363617483646f67")))
         ["cat" "dog"]))
  (is (= (->str (rlp/decode (->bytes "0x80"))) ""))
  (is (= (rlp/decode (->bytes "0xc0")) []))

  (is (= (->hex (rlp/decode (->bytes "0x0f"))) "0f"))
  (is (= (->hex (rlp/decode (->bytes "0x820400"))) "0400"))
  (is (= (rlp/decode (->bytes "0xc7c0c1c0c3c0c1c0")) [[], [[]], [[], [[]]]]))
  (is (= (->str (rlp/decode (->bytes "0xb8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974")))
         "Lorem ipsum dolor sit amet, consectetur adipisicing elit")))

