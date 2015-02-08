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
               :json-body doc)))
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
              :json-body body))
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
               :json-body body)))
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
      (let [index-term (if (string? index-name)
                         index-name
                         (s/join "," index-name))
            type-term  (when type
                         (if (string? type)
                           type
                           (s/join "," type)))
            params     (when (string? query)
                         {:q query})
            query      (when-not (string? query) query)]
        (call-es es
                 :delete (make-url index-term type-term "_query")
                 :json-body query
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
               :json-body body)))
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
               :json-body body)))
   good-status))
