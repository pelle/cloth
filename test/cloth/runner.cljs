(ns cloth.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            cloth.core-test
            cloth.util-test
            cloth.bytes-test
            cloth.keys-test
            cloth.tx-test
            cloth.net-test
            cloth.contracts-test
            cloth.filters-test
            cloth.rlp-test
            ))

(doo-tests
          ;'cloth.contracts-test
          ;'cloth.core-test
          ;'cloth.bytes-test
          ;'cloth.util-test
          'cloth.keys-test
          ; 'cloth.tx-test
          ; 'cloth.net-test
          ; 'cloth.filters-test
          ;'cloth.rlp-test
           )

