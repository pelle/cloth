# cloth

Simple ClojureScript (soon Clojure as well) library for interacting with the Ethereum blockchain.

Since it's still fairly early API is likely to change a lot in particular with regular clojure support.

Add the following to your project.clj file:

`[cloth "0.3.0"]`

Note I have not tested any of this in production or using minified clojurescript code.

## Basic usage

The `cloth.core` namespace has most of what you need for regular use for sending transactions.

```clojure
(require '[cloth.core :as cloth])
(require '[promesa.core :as p]) ;; promise library
```

### Check the balance of the current account:

```clojure
;; Both platforms
(p/then (cloth/balance) prn)

;; Clojure only
@(cloth/balance)
```

### Send transaction:

```clojure
;; Both platforms
(p/then (cloth/send-transaction {:to "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb"
                                 :value 100000000N}) prn)

;; Clojure only
@(cloth/send-transaction {:to "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb"
                          :value 100000000N})
```

### Signers

Signing is done by maps called signers and a multimethod called `(sign-with-signer tx signer)`.

The current implementation supports signers as KeyPair maps containing a private-key and an address:

```clojure
{:private-key "0x3fa3d2b5c94e3f521d6c160e0ef97123cc6d0946c12869b949959aa0f8c333de", 
 :address "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb"}
```

The `cloth.keys` namespace has a function `(create-keypair)` which creates a map like above.

#### Ethereum URL's

Or a url based signer which generates an ethereum-url primarily useful to create a link on a mobile browser or a QR code.

```clojure
{:type :url, 
 :address "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb" ;; optional
 :show-url (fn [url] 
                ;; trigger display of url in your web page
                ;; return a promise that is fullfilled based on a onhashchange event 
 )}
```

#### Proxy Signers

Proxy signers use simple smart contracts known as Proxy's that can be controlled through one or more `device keys` or other kinds of business rules.

A proxy contract needs to implement a function with the following interface:

```solidity
function forward(address recipient, uint value, bytes data) {
   // forward contract based on certain busines rules
}
```

Once you have your proxy contract deployed you can create a signer like this:

```clojure
{ :type :proxy
  :address "0xbfd6f4d8016d3b2388af8a6617778a3686993a1a" ;; address of proxy contract
  :device { :private-key "0x3fa3d2b5c94e3f521d6c160e0ef97123cc6d0946c12869b949959aa0f8c333de", 
            :address "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb"}}
```

#### Creating a global signer

In regular use with a single signer for example in a web app set it in a global signer atom `cloth.core/global-signer`:

```clojure
(reset! cloth.core/global-signer (cloth.keys/create-keypair))
```

For server applications you may want to assign a signer to a request using dynamic binding.

This is particularly useful in a ring-middleware:

```clojure

(defn extract-key-from-request [request]
  ;; App specific code
  ...)
  
(defn wrap-signer [app]
  (fn [request])
    (binding [cloth.core/bound-signer (extract-key-from-request request)]
      (app request)))
```

All code in the `cloth.core` namespace uses the `(cloth.core/current-signer)` function to return the current keypair map.

Instead of callbacks we use [promesa](http://funcool.github.io/promesa/latest/) which allow us to compose functions easily.

## Solidity Contracts

The `cloth.contracts` namespace allows you to compile solidity code and create clojure functions for each function in your solidity contract.

```clojure
;; For Clojure
(require '[cloth.contracts :as c])
;; For ClojureScript
(require '[cloth.contracts :as c :refer-macros [defcontract]])

(defcontract simple-token "test/cloth/SimpleToken.sol")

;; For simplicity sake the rest of the examples use Clojure @ syntax for dereferencing Promises

;; Deploy the solidity code to the blockchain
(def contract-address @(deploy-simple-token!))

;; Constant functions (that is for quering data from a smart contract)

@(circulation contract-address)
=> 0

;; Call a transaction function. Promise returns when it is mined
@(issue! contract recipient 123)

;; Check return value of a transaction function but doesn't actually create a transaction
@(issue? contract recipient 123)

;; Events in a solidity contract have a function created to create a core.async channel
;; Use regular promesa syntax here as this is likely to be done in a web interface
(require '[promesa.core :as p])

(defonce latest-message (atom nil))
(p/then (message-ch contract)
        (fn [{:keys [messages stop start] :as c}] 
            ; messages is a core.async channel
            ; stop is used to stop listening and start to restart it
          (go  (reset! latest-message (<! messages))
               (stop))

```

Note `defcontract` creates the functions in the namespace where it is called. 

To compile contracts you need `solc` the Solidity compiler installed.

## General ETH JSON-RPC like interface:

See the `cloth.chain` namespace which has bindings for most JSON-RPC calls that lets you query the blockchain.

Note I haven't clojurified all the responses yet. In particular the get block and get transaction-receipts are likely going to change.

## Configuring JSON-RPC endpoint

At the moment interaction with the Ethereum network is done through a JSON-RPC endpoint.

This default to http://localhost:8545

You can change it by resetting the `cloth.chain/ethereum-rpc` atom.

Eventually I want to add support for ethereumj for the clojure version.

## Development and Testing

Most code will work both on clojure and clojurescript. To run tests first install ethereumjs-testrpc:

```
npm install -g ethereumjs-testrpc
```

This is a small temporary test ethereum node. Run it:

```
testrpc -b 1
```

### Clojure tests

Run `lein test` or `lein test-refresh`.

### Clojurescript tests

We use [doo](https://github.com/bensu/doo) 

Follow instructions on above site

```
lein doo chrome test
```

## License

Copyright © 2016 Pelle Braendgaard

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
