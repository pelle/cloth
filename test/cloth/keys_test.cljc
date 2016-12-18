(ns cloth.keys-test
  (:require [cloth.keys :as keys]
            [cloth.bytes :as b]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

(deftest test-create-keypair
  (let [keypair (keys/create-keypair)]
    (is (= (set (keys keypair)) #{:private-key :address}))
    (is (not= keypair nil))))

(deftest test-keypair
  (is (= (keys/keypair "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b")
         {:private-key "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b"
          :address "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"})))

(deftest test-priv->address
  (is (= (keys/->address (keys/->public-key "0x0a72410411a3f4379db21742d3fb7e93611d4cce6000ee08b29a79bcb3943562"))
         "0x3563bfb5ece3a9cd41c90a4d6863f68358e6d52b"))
  (is (= (keys/->address (keys/->public-key "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b"))
         "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd")))

(deftest test->public-key
  (is (= (b/->hex (keys/->public-key "0xb23bcd7473ad6a707674db855bce324a2fe8d5bde5ce46fb5e15b704e8b0a9ad"))
         "0410289ff30f686e1ed76b6814ace80778dd604d002c9169b9f2054e4033582eae88745bc33198b30c90cc1b7207969ed1797dfd918024307a7f12918869be0d63")))

(deftest test->address
  (is (= (keys/->address {:private-key "0xb23bcd7473ad6a707674db855bce324a2fe8d5bde5ce46fb5e15b704e8b0a9ad"})
         "0x12bba453b15e896b70e62148b3b9b555d05fb2b4"))
  (is (= (keys/->address {:public-key "0x0410289ff30f686e1ed76b6814ace80778dd604d002c9169b9f2054e4033582eae88745bc33198b30c90cc1b7207969ed1797dfd918024307a7f12918869be0d63"})
         "0x12bba453b15e896b70e62148b3b9b555d05fb2b4"))
  (is (= (keys/->address "0x0410289ff30f686e1ed76b6814ace80778dd604d002c9169b9f2054e4033582eae88745bc33198b30c90cc1b7207969ed1797dfd918024307a7f12918869be0d63")
         "0x12bba453b15e896b70e62148b3b9b555d05fb2b4")))

(deftest test-get-private-key
  (let [kp (keys/create-keypair)]
    (is (= (keys/get-private-key kp) (:private-key kp)))
    (is (= (keys/get-private-key (:private-key kp)) (:private-key kp)))
    ))
