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
   (defn compile-solidity
     ([file ignore-warnings?]
      (let [result    (shell/sh "solc" "--combined-json" "abi,bin" file)
            failed?   (if ignore-warnings?
                        (not (zero? (:exit result)))
                        (not (c/blank? (:err result))))]
        (if failed?
          (throw (ex-info (:err result) {:solidity file :exit (:exit result)}))
          (json/parse-string (:out result) true))))
     ([file]
      (compile-solidity file false))))

#?(:clj
   (defn abi->args [f]
     (mapv #(symbol (c/kebab (:name %))) (:inputs f))))


(defn deploy-contract [binary fabi args]
  (->> (cloth/sign-and-send! (tx/create-contract-tx binary {} fabi args))
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

#?(:clj
   (defn compile-contract [contract file ignore-warnings?]
     "Compile a Solidity file and return a map of functions, constructor, events, and the name for a deploy function.
Tries to be compatible with older versions of solc that didn't include Solidity file name in the output of \"solc --combined-json=abi,bin\"."
     (let [{:keys [contracts]} (compile-solidity file ignore-warnings?)
           solidity-name                    (c/capitalize (c/camel (name contract)))               ;; convert, e.g. "simple-token" -> "SimpleToken"
           {json-abi :abi binary :bin}      (or (contracts (keyword solidity-name))                ;; old version of solc
                                                (contracts (keyword (str file ":" solidity-name))) ;; newer version of solc
                                                (throw (ex-info (str "Contract \"" solidity-name "\" not found in file \"" file "\".")
                                                                {:clojure-name  contract
                                                                 :solidity-name solidity-name})))
           abi                              (json/parse-string json-abi true)
           functions                        (filter #(= (:type %) "function") abi)
           constructor                      (first (filter #(= (:type %) "constructor") abi))
           events                           (filter #(= (:type %) "event") abi)
           deploy-name                      (symbol (str "deploy-" (c/kebab (name solidity-name)) "!"))]
       {:functions   functions
        :constructor constructor
        :events      events
        :binary      binary
        :deploy-name deploy-name})))

#?(:clj
   (defmacro defcontract
     "Compiles solidity and creates a set of functions in the current namespace:

     (defcontract stakeholder \"contracts/StakeHolder.sol/\")

     (deploy-stakeholder-tx {})

     For each function it creates a tx function to create a transaction:

     (add-device!! \"0x0sdsfafs...\" {})

     Parameters:
       contract           - the name of the contract converted to Clojure-style skewer-case (e.g.  \"SimpleToken\" becomes simple-token)
       file               - the Solidity file pathname (a string)
       ignore-warnings?   - optional, compiler warnings are OK but a compiler error will raise an exception
     "
     [contract file & [ignore-warnings?]]
     (let [{:keys [functions constructor events binary deploy-name]} (compile-contract contract file ignore-warnings?)]
       `(do
          (defn ~deploy-name [ & args# ]
            (deploy-contract ~binary ~constructor args#))

          ~@(for [f functions]
              (if (:constant f)
                `(defn ~(symbol (c/kebab (:name f)))
                       ~(str
                          "Calls "
                          (fn-doc f)
                          " without creating a transaction\nReturns a promise will return function return value")
                   [contract# & args#]
                   (call-contract-fn ~f contract# args#))
                `(do
                   (defn ~(symbol (str (c/kebab (:name f)) "!"))
                         ~(str "Calls " (fn-doc f) " and submit it as a transaction\nReturns a promise which will return the tx hash")
                     [contract# & args#]
                     (create-fn-tx ~f contract# args#))
                   (defn ~(symbol (str (c/kebab (:name f)) "!!"))
                         ~(str "Calls " (fn-doc f) " and submit it as a transaction.\nReturns a promise which will return the tx receipt once it has mined")
                     [contract# & args#]
                     (create-fn-and-wait-for-receipt ~f contract# args#))
                   (defn ~(symbol (str (c/kebab (:name f)) "?"))
                         ~(str "Calls " (fn-doc f) " without creating a transaction. Returns a promise which will return function return value")
                     [contract# & args#]
                     (call-contract-fn ~f contract# args#)))))

          ~@(for [e events]
              (do
                `(defn ~(symbol (str (c/kebab (:name e)) "-ch"))
                       ~(str
                           "Listen to event "
                           (fn-doc e)
                           "\nReturns a promise will return a map containing:\n :events a core.async channel\n :stop a function that stops listening to event.")
                   [contract# & args#]
                   (contract-event ~e contract# args#))))

          ))))


