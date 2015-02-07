(ns clj-es.client
  (:require [clj-es.core :refer :all]))

(def index
  (typical-call
   (fn
     ([es index-name type doc]
      (call-es es
               :post (make-url index-name type)
               :json-body doc))
     ([es index-name type id doc]
      (call-es es
               :put (make-url index-name type id)
               :json-body doc))
     ([es index-name type id create doc]
      (call-es es
               :put (make-url index-name type id (when create "_create"))
               :json-body doc)))
   good-status))
