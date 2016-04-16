(ns cloth.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            cloth.core-test
            cloth.util-test
            cloth.keys-test))

(doo-tests 'cloth.core-test
           'cloth.util-test
           'cloth.keys-test)

