(ns cloth.keys-test
  (:require [cloth.keys :as keys]
            [cloth.util :as util]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

#?(:cljs
   (deftest test-random-bytes
     (is (not= (keys/random-bytes) (keys/random-bytes)))))


#?(:cljs
   (deftest test-secp256k
     (is keys/secp256k1)))

#?(:cljs
   (deftest test-verify-private-key
     (is keys/verify-private-key)
     (is (not (keys/verify-private-key "sdfsdfs")))
     (let [key (keys/random-bytes)]
       (is (keys/verify-private-key key)))))

(deftest test-create-keypair
  (let [keypair (keys/create-keypair)]
    (is (= (keys keypair) '(:private-key :address)))
    (is (not= keypair nil))))

(deftest test-keypair
  (is (= (keys/keypair "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b")
         {:private-key "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b"
          :address "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"})))

(deftest test-get-private-key
  (let [private-key (keys/create-private-key)
        hex-private-key (util/->hex (keys/priv->b private-key))]
    (is (= (util/->hex (keys/priv->b (keys/get-private-key private-key))) hex-private-key))
    (is (= (util/->hex (keys/priv->b (keys/get-private-key hex-private-key))) hex-private-key))
    (is (= (util/->hex (keys/priv->b
                         (keys/get-private-key
                           (keys/keypair private-key)))) hex-private-key))))
