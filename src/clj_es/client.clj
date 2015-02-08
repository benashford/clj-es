(ns clj-es.client
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as s]
            [cheshire.core :as json]
            [clj-es.core :refer :all]))

;; Document APIs

(def index
  (typical-call
   (fn index*
     ([es index-name type doc]
      (index* es index-name type nil doc))
     ([es index-name type id doc]
      (call-es es
               (if id :put :post)
               (make-url index-name type id)
               :body doc)))
   good-status))

(def get
  (typical-call
   (fn get*
     ([es index-name id]
      (get* es index-name "_all" id))
     ([es index-name type id]
      (call-es es :get (make-url index-name type id))))
   good-or-not-found-status))

(def delete
  (typical-call
   (fn [es index-name type id]
     (call-es es :delete (make-url index-name type id)))
   good-or-not-found-status))

(def update
  (typical-call
   (fn [es index-name type id body]
     (call-es es
              :post (make-url index-name type id "_update")
              :body body))
   good-or-not-found-status))

(def multi-get
  (typical-call
   (fn multi-get*
     ([es body]
      (multi-get* es nil nil body))
     ([es index-name body]
      (multi-get* es index-name nil body))
     ([es index-name type body]
      (call-es es
               :get (make-url index-name type "_mget")
               :body body)))
   good-status))

(def bulk
  (typical-call
   (fn bulk*
     ([es ops]
      (bulk* es nil nil ops))
     ([es index-name ops]
      (bulk* es index-name nil ops))
     ([es index-name type ops]
      (call-es es
               :post (make-url index-name type "_bulk")
               :body (s/join ""
                             (map #(str (json/encode %) "\n") (flatten ops))))))
   good-status))

(def delete-by-query
  (typical-call
   (fn delete-by-query*
     ([es query]
      (delete-by-query* es "_all" query))
     ([es index-name query]
      (delete-by-query* es index-name nil query))
     ([es index-name type query]
      (let [[params
             query] (search-uri-or-body query)]
        (call-es es
                 :delete (make-url (multi index-name)
                                   (multi type)
                                   "_query")
                 :body query
                 :params params))))
   good-status))

(def termvectors
  (typical-call
   (fn term-vectors*
     ([es index-name type body]
      (term-vectors* es index-name type nil body))
     ([es index-name type id body]
      (call-es es
               :get (make-url index-name type id "_termvector")
               :body body)))
   good-status))

(def multi-termvectors
  (typical-call
   (fn multi-termvectors*
     ([es body]
      (multi-termvectors* es nil nil body))
     ([es index-name body]
      (multi-termvectors* es index-name nil body))
     ([es index-name type body]
      (call-es es
               :get (make-url index-name type "_mtermvectors")
               :body body)))
   good-status))

;; Search APIs

(def search
  (typical-call
   (fn search*
     ([es query]
      (search* es nil nil query))
     ([es index-name query]
      (search* es index-name nil query))
     ([es index-name type query]
      (let [[params
             query] (search-uri-or-body query)]
        (call-es es
                 :get (make-url (multi index-name)
                                (multi type)
                                "_search")
                 :body query
                 :params params))))
   good-status))

(def clear-scroll
  (typical-call
   (fn [es & scroll-ids]
     (call-es es
              :delete (make-url "_search" "scroll" (when (empty? scroll-ids) "_all"))
              :body (when-not (empty? scroll-ids) (multi scroll-ids))))
   good-status))
