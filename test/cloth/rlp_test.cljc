(ns cloth.rlp-test
    (:require [cloth.rlp :as rlp]
              [cloth.bytes :as b :refer [->bytes ->hex]]
      #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
          :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))

(defn encoded? [data expected]
      (let [result (->hex (byte-array (map #(unchecked-byte (int %)) expected)))
            encoded (->hex (rlp/encode data))]
           (if (= result encoded)
             true
             (do (println "Testing: " (prn-str expected))
                 (println "expected: " result)
                 (println "  actual: " encoded)
                 false))))

(defn normalized [data]
      (if (b/bytes? data)
        (->hex data)
        (mapv normalized data)))

(defn decoded? [data]
  (let [encoded (rlp/encode data)
        result (rlp/decode encoded)]
       (if (= (normalized result)
              (normalized data))
         true
         (do (println "Decoding: " (prn-str (normalized data)))
             (println "  actual: " (prn-str (normalized result)))
             (println "     rlp: " (->hex encoded))
             false))))

(deftest encode-tests
         (prn (rlp/->buf (->bytes "dog")))
         (is (encoded? (->bytes "dog") [0x83 \d \o \g]))
         (is (encoded? [(->bytes "cat") (->bytes "dog")] [0xc8, 0x83, \c, \a, \t, 0x83, \d, \o, \g]))
         (is (encoded? (->bytes "") [0x80]))
         (is (encoded? [] [0xc0]))
         (is (encoded? (->bytes "0f") [0x0f]))
         (is (encoded? (->bytes "0400") [0x82 0x04 0x00]))
         (is (encoded? [[], [[]], [[], [[]]]] [0xc7, 0xc0, 0xc1, 0xc0, 0xc3, 0xc0, 0xc1, 0xc0]))
         (is (encoded? (->bytes "Lorem ipsum dolor sit amet, consectetur adipisicing elit")
                       [0xb8 0x38 \L \o \r \e \m 0x20 \i \p \s \u \m 0x20 \d \o \l \o \r 0x20 \s \i \t 0x20 \a \m \e \t \,
                        0x20 \c \o \n \s \e \c \t \e \t \u \r 0x20 \a \d \i \p \i \s \i \c \i \n \g 0x20 \e \l \i \t]))
         )

(deftest decode-tests
         (is (decoded? (->bytes "dog")))
         (is (decoded? [(->bytes "cat") (->bytes "dog")]))
         (is (decoded? (->bytes "")))
         (is (decoded? []))
         (is (decoded? (->bytes "0f")))
         (is (decoded? (->bytes "0400")))
         (is (decoded? [[], [[]], [[], [[]]]] ))
         (is (decoded? (mapv ->bytes (range 60))))
         (is (decoded? (->bytes "Lorem ipsum dolor sit amet, consectetur adipisicing elit"))))
