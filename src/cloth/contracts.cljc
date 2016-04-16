(ns cloth.contracts
  #?(:clj (:require [clojure.java.shell :refer [sh]]
                    [cheshire.core :as json]
                    [cuerdas.core :as c]))
  #?(:cljs (:require
             [cloth.core]
             [cloth.tx]
             [cloth.chain])))


#?(:clj
   (defn compile-solidity [file]
     (json/parse-string (:out (sh "solc" "--combined-json" "abi,bin" file)) true)))



#?(:clj
   (defn abi->args [f]
     (mapv #(symbol (c/dasherize (:name %))) (:inputs f))))

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
           deploy-name (str "deploy-" (c/dasherize (name contract)) "!")]
       `(do
          (defn ~(symbol deploy-name) []
            (cloth.core/sign-and-send! (cloth.tx/create-contract-tx ~binary {})))

          ~@(for [f functions]
              (if (:constant f)
                `(defn ~(symbol (c/dasherize (:name f))) [contract & args]
                   (cloth.chain/call (cloth.tx/fn-tx contract ~f args)))
                `(defn ~(symbol (str (c/dasherize (:name f)) "!")) [contract & args]
                   (cloth.core/sign-and-send! (cloth.tx/fn-tx contract ~f args)))))))))


