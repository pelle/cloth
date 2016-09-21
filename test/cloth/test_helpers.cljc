(ns cloth.test-helpers
  (:require [cloth.core :as cloth]))

;; for testing use a faster polling rate
(reset! cloth/tx-poll-rate 1000)
