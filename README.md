# cloth

Simple ClojureScript (soon Clojure as well) library for interacting with the Ethereum blockchain.

This is extremely WIP and really should not be used by anyone yet. API is likely to change alot in particular with regular clojure support.

Add the following to your project.clj file:

`[cloth "0.2.3"]`

Note I have not tested any of this in production or using minified clojurescript code.

## Basic usage

The `cloth.core` namespace has most of what you need for regular use for sending transactions.

```clojure
(require '[cloth.core :as cloth])
(require '[promes.core :as p]) // promise library
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

### Key management

The current implementation supports KeyPair maps containing a private-key and an address:

```clojure
{:private-key "0x3fa3d2b5c94e3f521d6c160e0ef97123cc6d0946c12869b949959aa0f8c333de", 
 :address "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb"}
```

The `cloth.keys` has a function `(create-keypair)` which creates a map like above.

In regular use you can store a keypair in a global keypair atom `cloth.core/global-keypair`:

```clojure
(reset! cloth.core/global-keypair (cloth.keys/create-keypair))
```

For server applications you may want to assign a keypair to a request using dynamic binding.

This is particularly useful in a ring-middleware:

```clojure

(defn extract-key-from-request [request]
  ;; App specific code
  ...)
  
(defn wrap-signing-key [app]
  (fn [request])
    (binding [cloth.core/bound-keypair (extract-key-from-request request)]
      (app request)))
```

All code in the `cloth.core` namespace uses the `(cloth.core/keypair)` function to return the current keypair map.

Instead of callbacks we use [promesa](http://funcool.github.io/promesa/latest/) which allow us to compose functions easily.

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
testrpc
```

### Clojure tests

Run `lein test` or `lein test-refresh`.

### Clojurescript tests

We use [doo](https://github.com/bensu/doo) 

Install a js environment such as phantomjs

```
lein doo phantom test
```

## License

Copyright © 2016 Pelle Braendgaard

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
