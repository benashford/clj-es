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
    (if-not (empty? params)
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

(defn good-status [status]
  (and (>= status 200)
       (<  status 300)))

(defn good-or-not-found-status [status]
  (or (good-status status)
      (= status 404)))

(defn error-status [status]
  (and (>= status 500)
       (<  status 600)))

(defn- check-for-errors [{:keys [status error body] :as response}]
  (cond
   error                 response
   (error-status status) (assoc response :error body)
   :else                 response))

(defn- make-body [{:keys [json-body body]}]
  (if body
    body
    (if json-body
      (json/encode json-body))))

(def ^:dynamic params {})

(defmacro with-params [params & body]
  `(binding [params (merge params ~params)]
     ~@body))

(defn call-es [es method url-parts & {:as opts}]
  (let [es           (merge default-es es)
        url          (es-url es url-parts (merge params (:params opts)))
        http-options (make-http-options method url (make-body opts))
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

(defn parse-json [c]
  (a/map (fn [{:keys [error body] :as response}]
           (println "PARSE JSON:" (pr-str response))
           (if error
             response
             (assoc response :body (json/decode body true))))
         [c]))

(defn- check-status [c status-f]
  (a/map (fn [{:keys [status] :as response}]
           (if (status-f status)
             response
             (assoc response :error (str "Unexpected response:" status))))
         [c]))

(defn typical-call [f status-f]
  "The typical happy path that 90% of calls make"
  (fn [& args]
    (-> f
        (apply args)
        parse-json
        (check-status status-f))))

(defn make-url [& parts]
  (remove nil? parts))

(defn unwrap [response]
  (if-let [msg (:error response)]
    (throw (ex-info msg response))
    response))

(defmacro unwrap! [c]
  `(unwrap (a/<! ~c)))

(defmacro unwrap!! [c]
  `(unwrap (a/<!! ~c)))
