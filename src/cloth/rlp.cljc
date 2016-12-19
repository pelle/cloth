(ns cloth.rlp
  (:require [octet.core :as buf]
            [octet.buffer :as buffer]
            [octet.spec :as spec]
            [cloth.bytes :as b])
  #?(:clj
     (:import java.nio.ByteBuffer
              java.nio.ByteOrder
              io.netty.buffer.ByteBuf
              io.netty.buffer.ByteBufAllocator)))

;; Simple encoder decoder of RLP https://github.com/ethereum/wiki/wiki/RLP

(defn ->buf
  "Convert a byte array into an octet buffer"
  [ba]
  #?(:cljs (js/DataView. (.-buffer ba))
     :clj  (let [length (alength ba)
                 buff (buf/allocate length)]
             (.put buff ba)
             buff)
     ))

(defn ->ba [buff]
  #?(:cljs
          (js/Uint8Array. (.-buffer buff))
     :clj (.array buff)))

(declare rlp)

;; This whole section is because octet uses Int8Array by default

(defprotocol IBufferUBytes
  (read-ubytes [_ pos size] "Read a unsigned byte array."))

#?(:cljs
   (extend-type js/DataView
     IBufferUBytes
     (read-ubytes [buff pos size]
       (let [offset (.-byteOffset buff)
             buffer (.-buffer buff)]
         (js/Uint8Array. buffer (+ offset pos) size)))))

#?(:clj
   (extend-type java.nio.ByteBuffer
     IBufferUBytes
     (read-ubytes [buff pos size]
       (buffer/read-bytes buff pos size))))

#?(:clj
   (extend-type io.netty.buffer.ByteBuf
     IBufferUBytes
     (read-ubytes [buff pos size]
       (buffer/read-bytes buff pos size))))

(defn ubytes
  "Fixed size unsigned byte array type spec constructor."
  [^long size]
  (reify
    spec/ISpecSize
    (size [_] size)
    spec/ISpec
    (read [_ buff pos]
      [size (read-ubytes buff pos size)])

    (write [_ buff pos value]
      (buffer/write-bytes buff pos size value)
      size)))

(def rlp-bytes
  (reify
    spec/ISpecDynamicSize
    (size* [_ data]
      ;(println "rlp-bytes size*")
      (cond
        (and (= 1 (alength data))
             (< (aget data 0) 0x7f))
        1
        (<= (alength data) 55)
        (inc (alength data))
        :else
        (+ 1 (alength data) (spec/size* rlp-bytes (b/->bytes (alength data))))))

    spec/ISpec
    (read [_ buff pos]
      (let [[readed f] (spec/read (buf/ubyte) buff pos)]
        (cond
          (< f 0x80)
          (spec/read (buf/ubyte) buff pos)
          (< f 0xb7)
          (let [[readed data] (spec/read (ubytes (- f 0x80)) buff (inc pos))]
            [(inc readed) data])
          (< f 0xc0)
          (let [size-size (- f 0xb7)
                [_ size] (spec/read (ubytes size-size) buff (inc pos))
                [readed data] (spec/read (ubytes (b/->int size)) buff (+ (inc pos)
                                                                            size-size))]
            [(+ 1 size-size readed) data])
          :else (throw (ex-info "Unexpected data passed to rlp-bytearray reader" {})))))
    (write [_ buff pos data]
      ;(println "rlp-bytes write")
      ;(println "length: " (alength data))
      (cond
        (and (= 1 (alength data))
             (< (unchecked-byte (aget data 0)) (unchecked-byte 0x7f)))
        (do                                                 ;(println "Single byte: " (aget data 0))
          (spec/write (buf/ubyte) buff pos (aget data 0)))
        (<= (alength data) 55)
        (do                                                 ;(println "short bytes: " data)
          (+ (spec/write (buf/ubyte) buff pos (unchecked-byte (+ 0x80 (alength data))))
             (spec/write (buf/bytes (alength data)) buff (inc pos) data)))
        :else
        (let [size (b/->bytes (alength data))
              ;; I know awesome name
              size-size (spec/size* rlp-bytes size)]
          (+ (spec/write (buf/ubyte) buff pos (unchecked-byte (+ 0xb7 size-size)))
             (spec/write (buf/bytes size-size) buff (inc pos) size)
             (spec/write (buf/bytes (alength data)) buff (+ 1 size-size pos) data)))
        ))))

(def rlp-list
  (reify
    spec/ISpecDynamicSize
    (size* [_ data]
      ;(println "rlp-list size*")
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

          (let [size-size (- f 0xf7)
                [_ sizeb] (spec/read (buf/bytes size-size) buff (inc pos))
                total-size (b/->int sizeb)]
            (loop [size (inc size-size) pos (+ pos (inc size-size)) data []]
              (if (< size total-size)
                (let [[readed part] (spec/read rlp buff pos)]
                  (recur (+ size readed) (+ pos readed) (conj data part)))
                [size data])))

          )))
    (write [_ buff pos data]
      (let [size (spec/size* rlp data)]
        (if (<= size 55)
          (do (spec/write (buf/byte) buff pos (unchecked-byte (dec (+ 0xc0 size))))
              (reduce #(spec/write rlp buff (inc %1) %2) pos data))
          (let [sizeb (b/->bytes size)
                ;; I know awesome name
                size-size (spec/size* rlp-bytes sizeb)]
            (+ (spec/write (buf/ubyte) buff pos (unchecked-byte (+ 0xf7 size-size)))
               (spec/write (buf/bytes size-size) buff (inc pos) sizeb)
               (reduce (fn [current-size part]
                         (+ current-size (spec/write rlp buff current-size part)))
                       (+ size-size (inc pos))
                       data)))
          )))))


(def rlp
  (reify
    spec/ISpecDynamicSize
    (size* [_ data]
      (if (b/bytes? data)
        (spec/size* rlp-bytes data)
        (spec/size* rlp-list data)))
    spec/ISpec
    (read [_ buff pos]
      (let [[readed f] (spec/read (buf/ubyte) buff pos)]
        (if (< f 0xc0)
          (spec/read rlp-bytes buff pos)
          (spec/read rlp-list buff pos))))
    (write [_ buff pos data]
      (if (b/bytes? data)
        (spec/write rlp-bytes buff pos data)
        (spec/write rlp-list buff pos data)))))

(defn encode
  "Encodes an nested vector of byte array/buffers"
  [data]
  ;(println "encode: " data)
  (let [buf (buf/into rlp data)]
    (->ba buf)))

(defn decode
  "Decodes a rlp buffer or byte array into a nested structure of buffer" [ba]
  (buf/read (->buf ba) rlp))
