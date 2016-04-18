(ns cloth.net
  (:require
    #?@(:cljs [[httpurr.client.xhr :as http]])
    #?@(:clj [[httpurr.client.aleph :as http]
              [cheshire.core :as json]
              [byte-streams :as bytes]])
              [promesa.core :as p]))


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
    (throw error)
    (get-in response [:body :result])))

(defn rpc [endpoint method & params]
  (-> (http/post endpoint
                 (-> {:body (json-rpc-payload method params)}
                     json-encode
                     (assoc-in [:headers "Content-Type"] "application/json")))
      (p/then json-decode)
      (p/then json-rpc-response)))

(def ipfs-url "http://localhost:5001/api/v0/")

(defn ipfs-get
  "Returns the data associated with the given ipfs hash"
  [hash]
  (when hash
    (-> (http/get (str ipfs-url "cat/" hash))
        (p/then #(:body %)))))


(comment
  ;; Assuming ipfs is running locally with CORS set up
  ;; ipfs config --json API.HTTPHeaders.Access-Control-Allow-Origin '["*"]'
  ;; ipfs config --json API.HTTPHeaders.Access-Control-Allow-Methods '["PUT", "GET", "POST"]'
  ;; ipfs config --json API.HTTPHeaders.Access-Control-Allow-Credentials '["true"]'
  ;; ipfs daemon
  (p/then (ipfs-get "Qmf9qwT8eNeLGk41RfLfJJ7q8W891QkqDwVWV7aiqoVBxr"
                    #(println "response: " (prn-str %)))))


