(ns cloth.digests-test
  (:require
    [cloth.digests :as d]
    [cloth.bytes :as b]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures]]]
        :clj  [
    [clojure.test :refer [is are deftest testing use-fixtures]]])))


(deftest test-sha3
  (is (= (b/->hex (d/sha3 "hello"))
         "1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8")))

(deftest test-sha256
  (is (= (b/->hex (d/sha256 "hello"))
         "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")))

;(deftest test-ripemd160
;  (is (= (b/->hex (d/ripemd160 "hello"))
;         "108f07b8382412612c048d07d13f814118445acd")))
