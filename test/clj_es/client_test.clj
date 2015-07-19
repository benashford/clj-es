(ns clj-es.client-test
  (:require [clj-es.client :as client]
            [clj-es.core   :as core]
            [clojure.core.async :as a]
            [clojure.test :refer :all]))

;; Fixtures

(def ^:dynamic *client* nil)

(defn- set-client [f]
  (let [hostname (or (System/getenv "ES_HOST") "localhost")]
    (binding [*client* {:host hostname}]
      (f))))

(use-fixtures :once set-client)

;; Example data

(def example-doc
  {:str-field "one"
   :int-field 1})

;; Utilities

(defn- with-client [f & args]
  (apply f *client* args))

(def ^:private timeout-default 5000)

(defn- unwrp [c]
  (let [[val port] (a/alts!! [c (a/timeout timeout-default)])]
    (if (= port c)
      (core/unwrap val)
      (throw (ex-info "Timed-out" {})))))

(def with-client-unwrp (comp unwrp with-client))

;; Tests

(deftest index-test
  (let [index-res (with-client-unwrp client/index
                    "index-test"
                    "index-test-doc"
                    example-doc)]
    (is (= 201 (-> index-res :status)))
    (is (= true (-> index-res :body :created)))))
