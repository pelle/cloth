(ns cloth.keys-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            ethereumjs-tx
            [cloth.keys :as keys]
            [cloth.util :as util]))

(deftest test-random-bytes
  (is (not= (keys/random-bytes) (keys/random-bytes))))

(deftest test-secp256k
  (is keys/secp256k1))


(deftest test-verify-private-key
  (is keys/verify-private-key)
  (is (not (keys/verify-private-key "sdfsdfs")))
  (let [key (keys/random-bytes)]
    (is (keys/verify-private-key key))))

(deftest test-create-keypair
  (let [keypair (keys/create-keypair)]
    (is (= (keys keypair) '(:private-key :public-key :address)))
    (is (not= keypair nil))))

(deftest test-keypair
  (is (= (keys/keypair "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b")
         {:private-key "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b", :public-key "0x25e3692e41f97b510d3e6e6b02a3583256d19e5398248d00a39affed98fa69f1979335556d1bd7c142e1c06ca31b090973ad72f45243f3e9dcaa95980b715036", :address "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"}))
  (is (= (keys/keypair "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b" "0x25e3692e41f97b510d3e6e6b02a3583256d19e5398248d00a39affed98fa69f1979335556d1bd7c142e1c06ca31b090973ad72f45243f3e9dcaa95980b715036")
         {:private-key "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b", :public-key "0x25e3692e41f97b510d3e6e6b02a3583256d19e5398248d00a39affed98fa69f1979335556d1bd7c142e1c06ca31b090973ad72f45243f3e9dcaa95980b715036", :address "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"})))

(deftest test-get-private-key
  (let [private-key (keys/create-private-key)
        hex-private-key (util/->hex private-key)]
    (is (= (util/->hex (keys/get-private-key private-key)) hex-private-key))
    (is (= (util/->hex (keys/get-private-key hex-private-key)) hex-private-key))
    (is (= (util/->hex (keys/get-private-key (keys/keypair private-key))) hex-private-key))))

