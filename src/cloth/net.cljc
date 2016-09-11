(ns cloth.net
  (:require [promesa.core :as p]
            [httpurr.status :as s]
    #?@(:cljs [[httpurr.client.xhr :as http]])
    #?@(:clj [[httpurr.client.aleph :as http]
              [cheshire.core :as json]
              [byte-streams :as bytes]])))

(defn parse-json [d]
  #?(:cljs (js->clj (js/JSON.parse d) :keywordize-keys true))
  #?(:clj (json/parse-string (bytes/to-string d) true)))

(defn to-json [d]
  #?(:cljs (js/JSON.stringify (clj->js d)))
  #?(:clj (json/generate-string d)))

(defn json-decode
  [response]
  (update response :body parse-json))

(defn json-encode
  [request]
  (update request :body to-json))

(defn json-rpc-payload [method params]
  (let [payload {:method method :id 1 :jsonrpc "2.0"}]
    (if (seq params)
      (assoc payload :params params)
      payload)))

(defn json-rpc-response [response]
  (if-let [error (get-in response [:body :error])]
    (ex-info "json-rpc-error" error)
    (get-in response [:body :result])))

(defn process-response
  [response]
  (condp = (:status response)
    s/ok           (p/promise response)
    s/not-found    (p/rejected :not-found)
    s/unauthorized (p/rejected :unauthorized)))

(defn rpc [endpoint method & params]
  (-> (http/post endpoint
                 (-> {:body (json-rpc-payload method params)}
                     json-encode
                     (assoc-in [:headers "Content-Type"] "application/json")))
      ;(p/then process-response)
      (p/then json-decode)
      (p/then json-rpc-response)))
