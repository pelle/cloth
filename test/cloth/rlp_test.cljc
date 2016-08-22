(ns cloth.rlp-test
    (:require [cloth.rlp :as rlp]
              [cloth.util :as util :refer [hex-> ->hex]]
      #?@(:cljs [[cljs.test :refer-macros [is are deftest testing use-fixtures async]]]
          :clj  [[clojure.test :refer [is are deftest testing use-fixtures]]])))


(defn string->ba [string]
  #?(:cljs (util/Buffer. string)
     :clj  (.getBytes string)))

#?(:cljs
   (defn byte-array [a]
         (util/Buffer. (clj->js a))))

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
      (if (bytes? data)
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
         (prn (rlp/ba->buf (string->ba "dog")))
         (is (encoded? (string->ba "dog") [0x83 \d \o \g]))
         ;(is (encoded? [(string->ba "cat") (string->ba "dog")] [0xc8, 0x83, \c, \a, \t, 0x83, \d, \o, \g]))
         ;(is (encoded? (string->ba "") [0x80]))
         ;(is (encoded? [] [0xc0]))
         ;(is (encoded? (hex-> "0f") [0x0f]))
         ;(is (encoded? (hex-> "0400") [0x82 0x04 0x00]))
         ;(is (encoded? [[], [[]], [[], [[]]]] [0xc7, 0xc0, 0xc1, 0xc0, 0xc3, 0xc0, 0xc1, 0xc0]))
         ;(is (encoded? (string->ba "Lorem ipsum dolor sit amet, consectetur adipisicing elit")
         ;              [0xb8 0x38 \L \o \r \e \m 0x20 \i \p \s \u \m 0x20 \d \o \l \o \r 0x20 \s \i \t 0x20 \a \m \e \t \,
         ;               0x20 \c \o \n \s \e \c \t \e \t \u \r 0x20 \a \d \i \p \i \s \i \c \i \n \g 0x20 \e \l \i \t]))
         )

;(deftest decode-tests
;         (is (decoded? (string->ba "dog")))
;         (is (decoded? [(string->ba "cat") (string->ba "dog")]))
;         (is (decoded? (string->ba "")))
;         (is (decoded? []))
;         (is (decoded? (hex-> "0f")))
;         (is (decoded? (hex-> "0400")))
;         (is (decoded? [[], [[]], [[], [[]]]] ))
;         (is (decoded? (mapv util/int->b (range 60))))
;         (is (decoded? (string->ba "Lorem ipsum dolor sit amet, consectetur adipisicing elit"))))
