(ns clj-es.client
  (:refer-clojure :exclude [get])
  (:require [clj-es.core :refer :all]))

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
