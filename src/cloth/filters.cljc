(ns cloth.filters
  (:require [cloth.chain :as chain]
            [promesa.core :as p]
            [clojure.core.async :as async :refer [>! <! <!! go]]))

(defn filter-ch
  ([id]
   (filter-ch identity id))
  ([formatter id]
   (let [events (async/chan (async/sliding-buffer 100))
         poll   (atom true)
         speed  5000
         poller (fn []
                  (async/go-loop []
                                 (when @poll
                                   (-> (chain/filter-changes formatter id)
                                       (p/then
                                         (fn [items]
                                           (doall
                                             (for [item items]
                                               (do
                                                 (go (>! events item))))))))
                                   (<! (async/timeout speed))
                                   (recur))))]
     (poller)
     {:events events
      :start  (fn []
                (reset! poll true)
                (poller))
      :stop   #(reset! poll false)})))

(defn new-block-ch []
  (p/then (chain/new-block-filter)
          filter-ch))

(defn event-ch [query]
  (p/then (chain/new-filter query)
          filter-ch))
