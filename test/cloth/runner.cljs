(ns cloth.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            cloth.core-test
            cloth.util-test
            cloth.keys-test
            cloth.tx-test))

(doo-tests 'cloth.core-test
           'cloth.util-test
           'cloth.keys-test
           'cloth.tx-test)

