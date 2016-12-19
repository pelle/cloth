(ns cloth.filters
  (:require [cloth.chain :as chain]
            [promesa.core :as p]
            [cloth.bytes :as b]
            [cloth.core :as cloth]
    #?@(:cljs [[cljs.core.async :as async :refer [>! <!]]]
        :clj  [
            [clojure.core.async :as async :refer [>! <! <!! go go-loop]]])
            [cloth.util :as util]
            [cuerdas.core :as c])
  #?(:cljs (:require-macros [cljs.core.async.macros :as m :refer [go go-loop]])))

(defn filter-ch
  ([id]
   (filter-ch identity id))
  ([formatter id]
   (let [events (async/chan (async/sliding-buffer 100))
         poll   (atom true)
         speed  @cloth/tx-poll-rate
         poller (fn []
                  (go-loop []
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
                (when-not @poll
                  (reset! poll true)
                  (poller)))
      :stop   #(reset! poll false)})))


(defn new-block-ch []
  (p/then (chain/new-block-filter)
          filter-ch))

(defn event-ch [query]
  (let [parser (:parser query identity)
        formatter #(parser (chain/rpc->event %))]
    (p/then (chain/new-filter (dissoc query :parser))
            (partial filter-ch #(map formatter %)))))

(defn event-parser [inputs]
  (let [indexed (filter :indexed inputs)
        other (remove :indexed inputs)]
    (fn [event]
      (apply merge
             (conj (map (fn [i d]
                          {(keyword (c/kebab (name (:name i)))) (util/decode-solidity (:type i) d)})
                        indexed (rest (:topics event)))
                   (util/decode-return-value other (:data event) false))))))
