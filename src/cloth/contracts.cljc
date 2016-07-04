(ns cloth.contracts
  (:require
    [cloth.core :as cloth]
    [cloth.tx :as tx]
    [cloth.chain :as chain]
    [promesa.core :as p]
    [cuerdas.core :as c]
    #?@(:clj
        [[clojure.java.shell :as shell]
        [cheshire.core :as json]])))


#?(:clj
   (defn compile-solidity [file]
     (json/parse-string (:out (shell/sh "solc" "--combined-json" "abi,bin" file)) true)))

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
  (cloth.chain/call
    (cloth.tx/fn-tx contract fabi args)))

(defn create-fn-tx
  [fn-abi contract args]
  (cloth.core/sign-and-send!
    (cloth.tx/fn-tx contract fn-abi args)))

;; (<function name>!)
#?(:clj
   (defmacro defcontract
     "Compiles solidity and creates a set of functions in the current namespace:

     (defcontract stakeholder \"contracts/StakeHolder.sol/\")

     (deploy-stakeholder-tx {})

     For each function it creates a tx function to create a transaction:

     (add-device-tx \"0x0sdsfafs...\" {})

     "
     [contract file]
     (let [compiled (compile-solidity file)
           contract-key (first (keys (:contracts compiled))) ;(c/capitalize (c/camelize (name contract)))
           binary (get-in compiled [:contracts contract-key :bin])
           abi (json/parse-string (get-in compiled [:contracts contract-key :abi]) true)
           functions (filter #(= (:type %) "function") abi)
           deploy-name (symbol (str "deploy-" (c/dasherize (name contract)) "!"))]
       (println "all fns: " (map :name functions))
       `(do
          (defn ~deploy-name []
            (deploy-contract ~binary))

          ~@(for [f functions]
              (if (:constant f)
                `(defn ~(symbol (c/dasherize (:name f)))
                   [contract# & args#]
                   (call-contract-fn ~f contract# args#))
                `(defn ~(symbol (str (c/dasherize (:name f)) "!"))
                   [contract# & args#]
                   (create-fn-tx ~f contract# args#))))))))


