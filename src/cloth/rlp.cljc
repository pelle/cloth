(ns cloth.rlp
  (:require [octet.core :as buf]
            [octet.spec :as spec]
            [cloth.util :as util]))

;; Simple encoder decoder of RLP https://github.com/ethereum/wiki/wiki/RLP
(defn ba->buf
  "Convert a byte array into an octet buffer"
  [ba]
  (buf/allocate ba)
  )

(defn buf->ba [buff]
  (.array buff))

(declare rlp)

(def rlp-bytes
  (reify
    spec/ISpecDynamicSize
    (size* [_ data]
           (cond
             (and (= 1 (alength data))
                  (< (aget data 0) 0x7f))
               1
             (<= (alength data) 55)
               (inc (alength data))
             :else
             (+ 1 (alength data) (spec/size* rlp-bytes (util/int->b (alength data))))))

    spec/ISpec
    (read [_ buff pos]
          )

    (write [_ buff pos data]
           (cond
             (and (= 1 (alength data))
                  (< (aget data 0) 0x7f))
              (spec/write (buf/byte) buff pos (aget data 0))
             (<= (alength data) 55)
              (+ (spec/write (buf/byte) buff pos (unchecked-byte (+ 0x80 (alength data))))
                 (spec/write (buf/bytes (alength data)) buff (inc pos) data))
             :else
             (let [size (util/int->b (alength data))
                   ;; I know awesome name
                   size-size (spec/size* rlp-bytes size)]
                  (+ (spec/write (buf/byte) buff pos (unchecked-byte (+ 0xb7 size-size)))
                     (spec/write (buf/bytes size-size) buff (inc pos) size)
                     (spec/write (buf/bytes (alength data)) buff (+ 1 size-size pos) data)))
             ))))

(def rlp-list
  (reify
    spec/ISpecDynamicSize
    (size* [_ data]
           (let [size (reduce + (map (partial spec/size* rlp) data))]
                (cond
                  (<= size 55)
                  (inc (apply + (map #(spec/size* rlp %) data)))
                  :else
                  (inc (inc (apply + (map #(spec/size* rlp %) data)))) ;; TODO This only handles up to 255 bytes length right now
                  )))

    spec/ISpec
    (read [_ buff pos]
          )

    (write [_ buff pos data]
           (let [size (spec/size* rlp data)]
                (cond
                  (<= size 55)
                  (do (spec/write (buf/byte) buff pos (unchecked-byte (dec (+ 0xc0 size))))
                      (reduce #(spec/write rlp buff (inc %1) %2) pos data))
                  ;; TODO Implement for larger lists
                  )))))

(def rlp
      (reify
        spec/ISpecDynamicSize
        (size* [_ data]
               (if (bytes? data)
                 (spec/size* rlp-bytes data)
                 (spec/size* rlp-list data)))
        spec/ISpec
        (read [_ buff pos]
              (let [[readed f] (spec/read (rlp-bytes) buff pos)]
                   (cond
                     (< f 128) f
                     ;(< f 183) (buf/read (buf/read buff (inc pos) (- f 128)))
                     ;(= f 183) (buf/read (buf/read buff (+ 2 pos) (spec/read (buf/byte) buff (inc pos))))
                     )))
        (write [_ buff pos data]
               (if (bytes? data)
                 (spec/write rlp-bytes buff pos data)
                 (spec/write rlp-list buff pos data)))))

(defn encode
      "Encodes an nested vector of byte array/buffers" [ data ]
      (buf->ba (buf/into rlp data)))

(defn decode
      "Decodes a rlp buffer or byte array into a nested structure of buffer" [ ba ])
