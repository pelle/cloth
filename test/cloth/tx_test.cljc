(ns cloth.tx-test
  (:require [cloth.tx :as tx]
            [cloth.keys :as keys]
            [cloth.util :as util]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

(def kp {:private-key "0xa285ab66393c5fdda46d6fbad9e27fafd438254ab72ad5acb681a0e9f20f5d7b"
         :address "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"})

(deftest map->tx-test
  (is (= (tx/map->tx {}) {}))
  (is (= (tx/map->tx {:gas-price 123 :to "00"}) {"gasPrice" 123 "to" "0x00"})))

(deftest create-test
  (is (= (tx/tx->map (tx/create {})) {:to "0x00", :data nil, :nonce 0, :gas-price 0, :gas-limit 0, :value 0}))
  (is (= (tx/tx->map (tx/create {:to "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"
                                 :value 1231}))
         {:to "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd", :data nil, :nonce 0, :gas-price 0, :gas-limit 0, :value 1231}))
  (is (= (tx/tx->map (tx/create {:to "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"
                                 :nonce 1
                                 :gas-limit 123123
                                 :gas-price 2000
                                 :data "0x00"
                                 :value 1231}))
         {:to "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd", :data "0x00",:nonce 1, :gas-price 2000, :gas-limit 123123, :value 1231}))
  )

(deftest sign-test
  (let [private (keys/get-private-key kp)
        signed (-> (tx/create {:to        "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"
                           :nonce     1
                           :gas-limit 123123
                           :gas-price 2000
                           :data      "0x00"
                           :value     1231})
               (tx/sign private))]
    (is (= (tx/tx->map signed)
           {:to "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd",
            :from (:address kp)
            :data "0x00",:nonce 1, :gas-price 2000, :gas-limit 123123, :value 1231}))))

(deftest ->hex-test
  (let [private (keys/get-private-key kp)
        signed (-> (tx/create {:to        "0x2036c6cd85692f0fb2c26e6c6b2eced9e4478dfd"
                               :nonce     1
                               :gas-limit 123123
                               :gas-price 2000
                               :data      "0x00"
                               :value     1231})
                   (tx/sign private))]
    (is (= (tx/->hex signed)
           "0xf864018207d08301e0f3942036c6cd85692f0fb2c26e6c6b2eced9e4478dfd8204cf001ca0d8b8f26cf8951c7653137ea028b7a8dea03c75df792ece0c40401081eba24d2ca035d5b02f1372091e7aa1b251a803f6a4513e7dfbf58be22995a0ea61cdee6789"))))

;; TODO write tests for these
(deftest test-fn-tx)
(deftest test-contract-tx)
