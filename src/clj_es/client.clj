(ns clj-es.client
  (:require [clj-es.core :refer :all]))

(def index
  (typical-call
   (fn
     ([es index-name type doc]
      (index es index-name type nil nil doc))
     ([es index-name type params-or-id doc]
      (if (map? params-or-id)
        (index es index-name type nil params-or-id doc)
        (index es index-name type params-or-id nil doc)))
     ([es index-name type id params doc]
      (call-es es
               (if id :put :post)
               (make-url index-name type id)
               :params params
               :json-body doc)))
   good-status))
