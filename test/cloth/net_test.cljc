(ns cloth.net-test
  (:require
    [cloth.net :as net]
    [promesa.core :as p]
    #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
        :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))


(def ethereum-rpc "http://localhost:8545/")
(def ethrpc (partial net/rpc ethereum-rpc))

(deftest rpc-test
  #?(:cljs
     (async done
       (-> (ethrpc "web3_clientVersion")
           (p/timeout 1000)
           (p/then (fn [r]
                     (is (re-find #"TestRPC" r))
                     (done)))
           (p/catch (fn [e]
                      (is false (str "Did not return response"))
                      (done)))))
     :clj
     (let [req (ethrpc "web3_clientVersion")]
       (p/then req
               (fn [r]
                 (is (re-find #"TestRPC" r)))))))
