(ns cloth.rlp
  (:require [octet.core :as buf]
            [octet.spec :as spec]
            [cloth.util :as util]))

;; Simple encoder decoder of RLP https://github.com/ethereum/wiki/wiki/RLP

(defn ba->buf
  "Convert a byte array into an octet buffer"
  [ba]
      (let [buff (buf/allocate (alength ba))]
           (.put buff ba)))

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
          (let [[readed f] (spec/read (buf/ubyte) buff pos)]
               (cond
                 (< f 0x80)
                 (spec/read (buf/bytes 1) buff pos)
                 (< f 0xb7)
                 (let [[readed data] (spec/read (buf/bytes (- f 0x80)) buff (inc pos))]
                      [(inc readed) data])
                 (< f 0xc0)
                 (let [size-size (- f 0xb7)
                       [_ size] (spec/read (buf/bytes size-size) buff (inc pos))
                       [readed data] (spec/read (buf/bytes (util/b->int size)) buff (+ (inc pos)
                                                                                                size-size))]
                      [(+ 1 size-size readed) data])
                 :else (throw "Unexpected data passed to rlp-bytearray reader"))))
    (write [_ buff pos data]
           (cond
             (and (= 1 (alength data))
                  (< (unchecked-byte (aget data 0)) (unchecked-byte 0x7f)))
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
          (let [[readed f] (spec/read (buf/ubyte) buff pos)]
               (if (< f 0xf7)
                 (let [total-size (- f 0xc0)]
                      (loop [size 1 pos (inc pos) data []]
                            (if (<= size total-size)
                              (let [[readed part] (spec/read rlp buff pos)]
                                   (recur (+ size readed) (+ pos readed) (conj data part)))
                              [size data])))
                 )))
    (write [_ buff pos data]
           (let [size (spec/size* rlp data)]
                (if (<= size 55)
                  (do (spec/write (buf/byte) buff pos (unchecked-byte (dec (+ 0xc0 size))))
                      (reduce #(spec/write rlp buff (inc %1) %2) pos data))
                  (throw "Currently only supports lists up to 55 direct elements") ;; TODO Implement for larger lists
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
              (let [[readed f] (spec/read (buf/ubyte) buff pos)]
                   (if (< f 0xc0)
                     (spec/read rlp-bytes buff pos)
                     (spec/read rlp-list buff pos))))
        (write [_ buff pos data]
               (if (bytes? data)
                 (spec/write rlp-bytes buff pos data)
                 (spec/write rlp-list buff pos data)))))

(defn encode
      "Encodes an nested vector of byte array/buffers" [ data ]
      (buf->ba (buf/into rlp data)))

(defn decode
      "Decodes a rlp buffer or byte array into a nested structure of buffer" [ ba ]
      (buf/read (ba->buf ba) rlp))
