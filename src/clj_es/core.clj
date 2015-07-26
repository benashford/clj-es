;; Copyright 2015 Ben Ashford
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns clj-es.core
  (:require [clojure.string :as s]
            [clojure.core.async :as a]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))

;; ElasticSearch config

(def ^:private default-es
  {:host "localhost"
   :port 9200})

(defn- es-url
  "Given a map containing ElasticSearch configuration (e.g. host and port), the
   vector of parts of a URL, and an optional map containing parameters,
   construct an ElasticSearch URL."
  [{:keys [host port]} url-parts params]
  (let [url (format "http://%s:%s/%s"
                    host
                    port
                    (s/join "/" url-parts))]
    (if-not (empty? params)
      (format "%s?%s"
              url
              (s/join "&"
                      (for [[k v] params]
                        (str (name k) "=" v))))
      url)))

;; http-kit options

(def ^:private default-http-options
  {:keepalive 30000
   :timeout   5000})

(def ^:dynamic http-options {})

(defn- make-http-options
  "Produce the options map for http-kit for a specific request.  This merges the
   default map, the dynamic map, and adds any specific attributes needed for the
   request."
  [method url body]
  (let [options (-> (merge default-http-options
                           http-options)
                    (assoc :method method
                           :url url))]
    (if body
      (assoc options :body body)
      options)))

;; Status checks

(defn good-status [status]
  (and (>= status 200)
       (<  status 300)))

(defn good-or-not-found-status [status]
  (or (good-status status)
      (= status 404)))

(defn error-status [status]
  (and (>= status 500)
       (<  status 600)))

(defn- check-for-errors
  "Check a reponse for any errors, either specified in the error field or via an
   erroneous status code."
  [{:keys [status error body] :as response}]
  (cond
   error                 response
   (error-status status) (assoc response :error body)
   :else                 response))

(defn- make-body
  "Some rare API calls use text as the body, most others use JSON.  Convert any
   maps to JSON and leave strings as they are."
  [body]
  (when body
    (if (map? body)
      (json/encode body)
      body)))

;; URL parameters

(def ^:dynamic params {})

(defmacro with-params
  "Any ElasticSearch calls within this block will use these URL parameters."
  [params & body]
  `(binding [params (merge params ~params)]
     ~@body))

;; Calling ES

(defn call-es
  "All ElasticSearch requests come through here.  Given an map containing
   ElasticSearch configuration (e.g. host, port); an HTTP method, a collection
   defining the URL, and optionally one or both of: params (URL parameters), and
   body (either map or string containing the body of the request)."
  [es method url-parts & {:as opts}]
  (let [es           (merge default-es es)
        url          (es-url es url-parts (merge params (:params opts)))
        http-options (make-http-options method url (make-body (:body opts)))
        c            (a/chan)]
    (println "CALLING:" (pr-str http-options))
    (http/request http-options
                  (fn [response]
                    (println "RESPONSE:" (pr-str response))
                    (as-> response x
                          (dissoc x :headers :opts)
                          (check-for-errors x)
                          (a/put! c x))))
    c))

;; Post request handling.  Utilities for interpreting response objects.

(defn- parse-body [{:keys [error body] :as response}]
  (if error
    response
    (assoc response :body (json/decode body true))))

(defn- parse-json
  "Parse the body of a non-error response that comes through the given channel"
  [c]
  (a/go (parse-body (a/<! c))))

(defn- check-response-status [status-f {:keys [status] :as response}]
  (if (status-f status)
    response
    (assoc response :error (str "Unexpected response:" status))))

(defn- check-status [c status-f]
  (a/go (check-response-status status-f (a/<! c))))

;; Workflow

(defn typical-call [f status-f]
  "The typical happy path that 90% of calls make"
  (fn [& args]
    (-> f
        (apply args)
        parse-json
        (check-status status-f))))

;; Utilities

(defn make-url [& parts]
  (remove nil? parts))

(defn multi
  "For multi-index, or multi-type requests"
  [bits]
  (when bits
    (if (string? bits)
      bits
      (s/join "," bits))))

(defn search-uri-or-body [query]
  (let [query-string? (string? query)]
    [(when query-string?
       {:q query})
     (when-not query-string?
       query)]))

;; Unwrapping response

(defn unwrap [response]
  (if-let [msg (:error response)]
    (throw (ex-info msg response))
    response))

(defmacro unwrap! [c]
  `(unwrap (a/<! ~c)))

(defmacro unwrap!! [c]
  `(unwrap (a/<!! ~c)))
