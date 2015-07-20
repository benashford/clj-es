(ns clj-es.client-test
  (:require [clj-es.client :as client]
            [clj-es.core   :as core]
            [clojure.core.async :as a]
            [clojure.test :refer :all]))

;; Common utilities

(def ^:private timeout-default 5000)

(defn- unwrp [c]
  (let [[val port] (a/alts!! [c (a/timeout timeout-default)])]
    (if (= port c)
      (core/unwrap val)
      (throw (ex-info "Timed-out" {})))))

;; Fixtures

(def ^:dynamic *client* nil)

(defn- set-client [f]
  (let [hostname (or (System/getenv "ES_HOST") "localhost")]
    (binding [*client* {:host hostname}]
      (f))))

(defn- clear-es [f]
  (unwrp (client/delete *client*))
  (f))

(use-fixtures :once set-client clear-es)

;; Example data

(def example-doc
  {:str-field "one"
   :int-field 1})

;; Utilities

(defn- with-client [f & args]
  (apply f *client* args))

(def with-client-unwrp (comp unwrp with-client))

;; Tests

(deftest index-test
  (testing "auto-generated ID"
    (let [index-res (with-client-unwrp client/index
                      "index-test"
                      "index-test-doc"
                      example-doc)]
      (is (= 201 (-> index-res :status)))
      (is (= true (-> index-res :body :created)))))
  (testing "specific ID"
    (let [index-res (with-client-unwrp client/index
                      "index-test"
                      "index-test-doc"
                      "1"
                      example-doc)]
      (is (= 201 (-> index-res :status)))
      (is (= true (-> index-res :body :created))))))
