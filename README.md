# cloth

Simple ClojureScript (soon Clojure as well) library for interacting with the Ethereum blockchain.

This is extremely WIP and really should not be used by anyone yet. API is likely to change alot in particular with regular clojure support.

## Basic usage




Basic idea for use int the browser is that you will always have one keypair stored in the

`cloth.core/keychain` atom.

```clojure
(require '[cloth.core :as cloth])

> (cloth/maybe-create-keypair)
> @keypair
{:private-key "0x3fa3d2b5c94e3f521d6c160e0ef97123cc6d0946c12869b949959aa0f8c333de", :public-key "0x2bc20e2d35874f64f1115351f70a0c9013cd3807707cf7d62b99bd1439a3ed54611d8a976c2637a997b0927670bbc12448b79578e609b91da148917f14be8be4", :address "0x9927ff21b9bb0eee9b0ee4867ebf9102d12d6ecb"}

(p/then (cloth/faucet! 100000) prn)

```

Instead of callbacks we use [promesa](http://funcool.github.io/promesa/latest/) which allow us to compose functions easily.

## Testing

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
