(ns cloth.rlp-test
    (:require [cloth.rlp :as rlp]
              [cloth.util :as util :refer [hex-> ->hex]]
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

(deftest encode-tests
         (is (encoded? (.getBytes "dog") [0x83 \d \o \g]))
         (is (encoded? [(.getBytes "cat") (.getBytes "dog")] [0xc8, 0x83, \c, \a, \t, 0x83, \d, \o, \g]))
         (is (encoded? (.getBytes "") [0x80]))
         (is (encoded? [] [0xc0]))
         (is (encoded? (byte-array [0x0f]) [0x0f]))
         (is (encoded? (byte-array [0x04 0x00]) [0x82 0x04 0x00]))
         (is (encoded? [[], [[]], [[], [[]]]] [0xc7, 0xc0, 0xc1, 0xc0, 0xc3, 0xc0, 0xc1, 0xc0]))
         (is (encoded? (.getBytes "Lorem ipsum dolor sit amet, consectetur adipisicing elit")
                       [0xb8 0x38 \L \o \r \e \m 0x20 \i \p \s \u \m 0x20 \d \o \l \o \r 0x20 \s \i \t 0x20 \a \m \e \t \,
                        0x20 \c \o \n \s \e \c \t \e \t \u \r 0x20 \a \d \i \p \i \s \i \c \i \n \g 0x20 \e \l \i \t])))
