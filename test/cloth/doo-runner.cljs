(ns cloth.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [cloth.core-test]))

(doo-tests 'cloth.core-test)

