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

(defn- make-documents
  "Make some test data"
  [index-name doc-type & ids]
  (core/unwrap!!
   (client/bulk *client*
                index-name
                doc-type
                (map (fn [id]
                       [{:index {:_id id}}
                        {:str-field (str id)
                         :int-field id}])
                     ids))))

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

(deftest get-test
  (let [doc (make-documents "get-test" "get-test-doc" 1)]
    (let [get-res (with-client-unwrp client/get
                    "get-test"
                    "get-test-doc"
                    1)]
      (is (= 200 (-> get-res :status)))
      (is (= true (-> get-res :body :found)))
      (is (= {:str-field "1" :int-field 1} (-> get-res :body :_source))))
    (let [get-res (with-client-unwrp client/get
                    "get-test"
                    "get-test-doc"
                    2)]
      (is (= 404 (-> get-res :status)))
      (is (= false (-> get-res :body :found))))))

(deftest delete-test
  (let [doc (make-documents "delete-test" "delete-test-doc" 1 2)]
    (let [delete-res (with-client-unwrp client/delete
                       "delete-test"
                       "delete-test-doc"
                       1)
          get-1-res  (with-client-unwrp client/get
                       "delete-test"
                       "delete-test-doc"
                       1)
          get-2-res  (with-client-unwrp client/get
                       "delete-test"
                       "delete-test-doc"
                       2)]
      (is (= 200 (-> delete-res :status)))
      (is (= "1" (-> delete-res :body :_id)))
      (is (= false (-> get-1-res :body :found)))
      (is (= true (-> get-2-res :body :found))))))
