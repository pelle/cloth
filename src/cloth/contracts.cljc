(ns cloth.contracts
  (:require
    [cloth.core :as cloth]
    [cloth.tx :as tx]
    [cloth.util :as util]
    [cloth.chain :as chain]
    [cloth.filters :as filters]
    [promesa.core :as p]
    [cuerdas.core :as c]
    #?@(:clj
        [
    [clojure.core.async :as async]
    [clojure.java.shell :as shell]
    [cheshire.core :as json]])
    [cloth.util :as util]))


#?(:clj
   (defn compile-solidity [file]
     (let [result (shell/sh "solc" "--combined-json" "abi,bin" file)]
       (if (not (c/blank? (:err result)))
         (throw (ex-info (:err result) {:solidity file :exit (:exit result)}))
         (json/parse-string (:out result) true)))))

#?(:clj
   (defn abi->args [f]
     (mapv #(symbol (c/dasherize (:name %))) (:inputs f))))


(defn deploy-contract [binary]
  (->> (cloth/sign-and-send! (tx/create-contract-tx binary {}))
       (p/mapcat cloth/when-mined)
       (p/mapcat chain/get-transaction-receipt)
       (p/mapcat (fn [receipt] (p/resolved (:contract-address receipt))))))

(defn call-contract-fn
  [fabi contract args]
  (->> (cloth.chain/call
         (assoc (cloth.tx/fn-tx contract fabi args)
           :from (:address (cloth/current-signer))))
       (p/mapcat (fn [val]
                   ;(println "returned: " val)
                   (p/resolved (util/decode-return-value (:outputs fabi) val))
                   ))))


(defn create-fn-tx
  [fn-abi contract args]
  (cloth.core/sign-and-send!
    (cloth.tx/fn-tx contract fn-abi args)))

(defn create-fn-and-wait-for-receipt
  [fn-abi contract args]
  (->> (create-fn-tx fn-abi contract args)
       (p/mapcat chain/get-transaction-receipt)))

(defn contract-event
  [fn-abi contract args]
  (let [parser (filters/event-parser (:inputs fn-abi))]
    (filters/event-ch {:address contract
                       :topics [(util/encode-event-sig (:name fn-abi) (map :type (:inputs fn-abi)))]
                       :parser parser})))

(defn fn-doc [ fn-abi]
      (let [fncall (str (:name fn-abi) "(" (c/join ", " (map #(str (:type %) " " (:name % (:type %))) (:inputs fn-abi))) ")")
            const (if (:constant fn-abi) " constant")
            returns (if (not (empty? (:outputs fn-abi)))
                      (str " returns(" (c/join ", " (map #(str (:type %) (if (:name %) (str " " (:name %)))) (:outputs fn-abi))) ")"))
            ]
           (str fncall const returns)))

;; (<function name>!)
#?(:clj
   (defmacro defcontract
     "Compiles solidity and creates a set of functions in the current namespace:

     (defcontract stakeholder \"contracts/StakeHolder.sol/\")

     (deploy-stakeholder-tx {})

     For each function it creates a tx function to create a transaction:

     (add-device!! \"0x0sdsfafs...\" {})

     "
     [contract file]
     (let [compiled (compile-solidity file)
           contract-key (keyword (c/capitalize (c/camelize (name contract))))
           binary (get-in compiled [:contracts contract-key :bin])
           abi (json/parse-string (get-in compiled [:contracts contract-key :abi]) true)
           functions (filter #(= (:type %) "function") abi)
           events (filter #(= (:type %) "event") abi)
           deploy-name (symbol (str "deploy-" (c/dasherize (name contract)) "!"))]
       `(do
          (defn ~deploy-name []
            (deploy-contract ~binary))

          ~@(for [f functions]
              (if (:constant f)
                `(defn ~(symbol (c/dasherize (:name f)))
                       ~(str
                          "Calls "
                          (fn-doc f)
                          " without creating a transaction\nReturns a promise will return function return value")
                   [contract# & args#]
                   (call-contract-fn ~f contract# args#))
                `(do
                   (defn ~(symbol (str (c/dasherize (:name f)) "!"))
                         ~(str "Calls " (fn-doc f) " and submit it as a transaction\nReturns a promise which will return the tx hash")
                     [contract# & args#]
                     (create-fn-tx ~f contract# args#))
                   (defn ~(symbol (str (c/dasherize (:name f)) "!!"))
                         ~(str "Calls " (fn-doc f) " and submit it as a transaction.\nReturns a promise which will return the tx receipt once it has mined")
                     [contract# & args#]
                     (create-fn-and-wait-for-receipt ~f contract# args#))
                   (defn ~(symbol (str (c/dasherize (:name f)) "?"))
                         ~(str "Calls " (fn-doc f) " without creating a transaction. Returns a promise which will return function return value")
                     [contract# & args#]
                     (call-contract-fn ~f contract# args#)))))

          ~@(for [e events]
              (do
                `(defn ~(symbol (str (c/dasherize (:name e)) "-ch"))
                       ~(str
                           "Listen to event "
                           (fn-doc e)
                           "\nReturns a promise will return a map containing:\n :events a core.async channel\n :stop a function that stops listening to event.")
                   [contract# & args#]
                   (contract-event ~e contract# args#))))

          ))))


