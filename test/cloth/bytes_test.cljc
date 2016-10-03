(ns cloth.bytes-test
  (:require [cloth.bytes :as b]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures]]
               [goog.math.Integer :as Integer]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

(defn eq
  [a b]
  #?(:clj  (.equals a (biginteger b))
     :cljs (.equals a (Integer/fromNumber b))))

(defn hex= [bytes hex]
  (= (b/->hex bytes) hex))

(deftest test-add0x
  (is (= (b/add0x "ab0c") "0xab0c"))
  (is (= (b/add0x "0xab0c") "0xab0c")))


(deftest test-strip0x
  (is (= (b/strip0x "ab0c") "ab0c"))
  (is (= (b/strip0x "0xab0c") "ab0c")))

(deftest test-hex?
  (is (b/hex? "00"))
  (is (b/hex? "0x00"))
  (is (b/hex? "1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8")))

(deftest test->hex
  (is (= (b/->hex (b/byte-array 0)) ""))
  (is (= (b/->hex (b/byte-array 4)) "00000000"))
  (is (= (b/->hex "00000000") "00000000"))
  (is (= (b/->hex "0x00000000") "00000000"))
  (is (= (b/->hex "Hello") "48656c6c6f")))

(deftest test->bytes
  (is (hex= (b/->bytes (b/byte-array 0)) ""))
  (is (hex= (b/->bytes (b/byte-array 4)) "00000000"))
  (is (hex= (b/->bytes "00000000") "00000000"))
  (is (hex= (b/->bytes "0x00000000") "00000000"))
  (is (hex= (b/->bytes "Hello") "48656c6c6f")))

(deftest test-rpad
  (is (= (b/->hex (b/rpad (b/->bytes "ab") 4)) "ab000000"))
  (is (= (b/->hex (b/rpad (b/->bytes "ab001010") 4)) "ab001010"))
  (is (= (b/->hex (b/rpad (b/->bytes "ab001010") 2)) "1010")))

(deftest test-pad
  (is (= (b/->hex (b/pad (b/->bytes "ab") 4)) "000000ab"))
  (is (= (b/->hex (b/pad (b/->bytes "ab001010") 4)) "ab001010"))
  (is (= (b/->hex (b/pad (b/->bytes "ab001010") 2)) "1010")))

(deftest test->int
  (is (eq (b/->int "0x0") 0))
  (is (eq (b/->int "0x00") 0))
  (is (eq (b/->int "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00") -256))
  (is (eq (b/->int "0x00ff00") 65280))
  (is (eq (b/->int "00") 0))
  (is (eq (b/->int "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00") -256))
  (is (eq (b/->int "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff") -1))
  (is (eq (b/->int "00ff00") 65280)))

(deftest test->uint
  (is (eq (b/->uint "0x0") 0))
  (is (eq (b/->uint "0x00") 0))
  (is (eq (b/->uint "0xff00") 65280))
  (is (eq (b/->uint "00") 0))
  (is (eq (b/->uint "ff00") 65280)))

(deftest test-int->hex
  (is (= (b/int->hex 0) ""))
  (is (= (b/int->hex 1) "01"))
  (is (= (b/int->hex 65280) "ff00")))
