(ns cloth.rlp
  (:require [octet.core :as buf]
            [octet.spec :as spec]))


(defn rpl-spec
  (reify
    spec/ISpec
    (read [_ buff pos]
      (let [ [readed f] (spec/read (buf/byte) buff pos)]
        (cond
          (< f 128)  f
          (< f 183)  (spec/read (buf/read-bytes buff (inc pos) (- f 128)))
          (= f 183)  (spec/read (buf/read-bytes buff (+ 2 pos) (spec/read (buf/byte) buff (inc pos))))
          )))

    (write [_ buff pos data])))
