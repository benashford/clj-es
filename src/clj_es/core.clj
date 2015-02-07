(ns clj-es.core
  (:require [clojure.string :as s]
            [clojure.core.async :as a]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))

(def ^:private default-es
  {:host "localhost"
   :port 9200})

(defn- es-url [{:keys [host port]} url-parts params]
  (let [url (format "http://%s:%s/%s"
                    host
                    port
                    (s/join "/" url-parts))]
    (if params
      (format "%s?%s"
              url
              (s/join "&"
                      (for [[k v] params]
                        (str (name k) "=" v))))
      url)))

(def ^:private default-http-options
  {:keepalive 30000
   :timeout   5000})

(def ^:dynamic http-options {})

(defn- make-http-options [method url body]
  (let [options (-> (merge default-http-options
                           http-options)
                    (assoc :method method
                           :url url))]
    (if body
      (assoc options :body body)
      options)))

(defn- error-status [status]
  (and (>= status 500)
       (<  status 600)))

(defn- check-for-errors [{:keys [status error body] :as response}]
  (cond
   error                 response
   (error-status status) (assoc response :error body)
   :else                 response))

(defn call-es [es method url-parts & {:as opts}]
  (let [es           (merge default-es es)
        url          (es-url es url-parts (:params opts))
        http-options (make-http-options method url (:body opts))
        c            (a/chan)]
    (http/request http-options
                  (fn [response]
                    (as-> response x
                          (dissoc x :headers :opts)
                          (check-for-errors x)
                          (a/put! c x))))
    c))

(defn parse-json [c]
  (a/map (fn [{:keys [error body] :as response}]
           (if error
             response
             (assoc response :body (json/decode body true))))
         [c]))
