(ns cloth.util-test
  (:require [cloth.util :as util]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

(deftest test-add0x
  (is (= (util/add0x "ab0c") "0xab0c"))
  (is (= (util/add0x "0xab0c") "0xab0c")))

(deftest test-strip0x
  (is (= (util/strip0x "ab0c") "ab0c"))
  (is (= (util/strip0x "0xab0c") "ab0c")))

(deftest test-sha3
  (is (= (util/->hex (util/sha3 "hello")) "1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8")))

(deftest test-hex->int
  (is (= (util/hex->int "0x00") 0))
  (is (= (util/hex->int "0xff00") 65280))
  (is (= (util/hex->int "00") 0))
  (is (= (util/hex->int "ff00") 65280)))

(deftest test-int->hex
  (is (= (util/int->hex 0) "00"))
  (is (= (util/int->hex 65280) "ff00")))

(deftest test-rpad
  (is (= (util/->hex (util/rpad (util/hex-> "ab") 4)) "ab000000"))
  (is (= (util/->hex (util/rpad (util/hex-> "ab001010") 4)) "ab001010"))
  (is (= (util/->hex (util/rpad (util/hex-> "ab001010") 2)) "1010")))

(deftest test-pad
  (is (= (util/->hex (util/pad (util/hex-> "ab") 4)) "000000ab"))
  (is (= (util/->hex (util/pad (util/hex-> "ab001010") 4)) "ab001010"))
  (is (= (util/->hex (util/pad (util/hex-> "ab001010") 2)) "1010")))
