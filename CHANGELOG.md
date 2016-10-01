# CLOTH Changelog

### 0.3.1

- Support contract constructor arguments
- RPC errors actually throw errors now @hookercookerman
- Bug when encoding fnsig @hookercookerman

### 0.3.0

- Introduces concept of `signer` BREAKING CHANGE
    - Support ethereum URL's
    - Supports Proxy Contracts
- Fixes issues with bytes32
- Supports array return values
- Make polling times adjustable

### 0.2.7

- Fix `bytes` and `bytesXX` encoding and decoding of solidity function call parameters
    - Accepts "0x" prefixed hex string which gets decoded and converted to bytearray/Buffer
    - Any other strings are converted to bytearray or Buffer
    - bytearray/Buffers are directly accepted
    - decoding always returns a bytearray or Buffer depending on Clojure or ClojureScript
- Added Clojure Docs for all functions generated from Solidity. This can be improved but it's better than nothing.
- More robust solidity compilation process
- Now correctly picks the solidity contract when compiling based on multiple sourcefile dependencies in `(defcontract)`
    - Correctly means means that it assumes the Solidity contract is the Capitalized camelized name of the contract name passed into `(defcontract name path)`
