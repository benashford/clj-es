;; Copyright 2015 Ben Ashford
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns clj-es.client
  (:refer-clojure :exclude [count
                            get
                            flush
                            update])
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
   (fn delete*
     ([es]
      (call-es es :delete (make-url "_all")))
     ([es index-name type id]
      (call-es es :delete (make-url index-name type id))))
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

(def template-search
  "Call a templated search"
  (typical-call
   (fn [es body]
     (call-es es
              :get (make-url "_search" "template")
              :body body))
   good-status))

(def search-template
  "Get and set templates"
  (typical-call
   (fn
     ([es template-name]
      (call-es es
               :get (make-url "_search" "template" template-name)))
     ([es template-name body]
      (call-es es
               :post (make-url "_search" "template" template-name)
               :body body)))
   good-or-not-found-status))

(def search-shards
  (typical-call
   (fn [es index-name]
     (call-es es :get (make-url index-name "_search_shards")))
   good-status))

(def suggest
  (typical-call
   (fn suggest*
     ([es body]
      (suggest* es nil body))
     ([es index-name body]
      (call-es es :post (make-url index-name "_suggest") :body body)))
   good-status))

(def multi-search
  (typical-call
   (fn multi-search*
     ([es ops]
      (multi-search* nil nil ops))
     ([es index-name ops]
      (multi-search* index-name nil ops))
     ([es index-name type ops]
      (call-es es
               :get (make-url index-name type "_msearch")
               :body (s/join ""
                             (map #(str (json/encode %) "\n") (flatten ops))))))
   good-status))

(def count
  (typical-call
   (fn count*
     ([es query]
      (count* es nil nil query))
     ([es index-name query]
      (count* es index-name nil query))
     ([es index-name type query]
      (let [[params
             query] (search-uri-or-body query)]
        (call-es es
                 :get (make-url (multi index-name)
                                (multi type)
                                "_count")
                 :body query
                 :params params))))
   good-status))

(def search-exists
  (typical-call
   (fn search-exists*
     ([es query]
      (search-exists* es nil nil query))
     ([es index-name query]
      (search-exists* es index-name nil query))
     ([es index-name type query]
      (let [[params
             query] (search-uri-or-body query)]
        (call-es es
                 :get (make-url (multi index-name)
                                (multi type)
                                "_search"
                                "exists")
                 :body query
                 :params params))))
   good-status))

(def validate
  (typical-call
   (fn validate*
     ([es query]
      (validate* es nil nil query))
     ([es index-name query]
      (validate* es index-name nil query))
     ([es index-name type query]
      (let [[params
             query] (search-uri-or-body query)]
        (call-es es
                 :get (make-url (multi index-name)
                                (multi type)
                                "_validate"
                                "query")
                 :body query
                 :params params))))
   good-status))

(def explain
  (typical-call
   (fn explain*
     ([es index-name type id query]
      (let [[params
             query] (search-uri-or-body query)]
        (call-es es
                 :get (make-url index-name
                                type
                                id
                                "_explain")
                 :body query
                 :params params))))
   good-status))

;; Percolation

(def percolate
  (typical-call
   (fn [es index-name type document-or-id]
     (let [document (when (map? document-or-id) document-or-id)
           id       (when-not (map? document-or-id) document-or-id)]
       (call-es es
                :get (make-url index-name type id "_percolate")
                :body document)))
   good-status))

(def percolate-count
  (typical-call
   (fn [es index-name type document]
     (call-es es
              :get (make-url index-name type "_percolate" "count")
              :body document))
   good-status))

(def multi-percolate
  (typical-call
   (fn multi-percolate*
     ([es ops]
      (multi-percolate* es nil nil ops))
     ([es index-name ops]
      (multi-percolate* es index-name nil ops))
     ([es index-name type ops]
      (call-es es
               :get (make-url index-name type "_mpercolate")
               :body (s/join ""
                             (map #(str (json/encode %) "\n") (flatten ops))))))
   good-status))

(def more-like-this
  (typical-call
   (fn [es index-name type id & [fields params]]
     (call-es es
              :get (make-url index-name type id "_mlt")
              :params (merge {}
                             params
                             (when fields
                               {:mlt_fields (multi fields)}))))
   good-status))

;; Index APIs

(def create-index
  (typical-call
   (fn [es index-name & [body]]
     (call-es es
              :put (make-url index-name)
              :body body))
   good-status))

(def delete-index
  (typical-call
   (fn delete-index*
     ([es]
      (delete-index* es "_all"))
     ([es index-name]
      (call-es es :delete (make-url (multi index-name)))))
   good-status))

(def get-index
  (typical-call
   (fn get-index*
     ([es]
      (get-index* es "_all"))
     ([es index-name & [features]]
      (call-es es :get (make-url (multi index-name) (multi features)))))
   good-or-not-found-status))

(def index-exists
  (typical-call
   (fn [es index-name]
     (call-es es :head (make-url index-name)))
   good-or-not-found-status))

(def open-index
  (typical-call
   (fn open-index*
     ([es]
      (open-index* es "_all"))
     ([es index-name]
      (call-es es :post (make-url (multi index-name) "_open"))))
   good-status))

(def close-index
  (typical-call
   (fn close-index*
     ([es]
      (close-index* es "_all"))
     ([es index-name]
      (call-es es :post (make-url (multi index-name) "_close"))))
   good-status))

(def put-mapping
  (typical-call
   (fn put-mapping*
     ([es type mapping]
      (put-mapping* es "_all" type mapping))
     ([es index-name type mapping]
      (call-es es :put (make-url (multi index-name)
                                 "_mapping"
                                 type)
               :body mapping)))
   good-status))

(def get-mapping
  (typical-call
   (fn get-mapping*
     ([es]
      (get-mapping* es "_all" nil nil))
     ([es index-name]
      (get-mapping* es index-name nil nil))
     ([es index-name type]
      (get-mapping* es index-name type nil))
     ([es index-name type field]
      (call-es es :get (make-url (multi index-name)
                                 "_mapping"
                                 (multi type)
                                 (when field
                                   "field")
                                 (multi field)))))
   good-status))

(def type-exists
  (typical-call
   (fn [es index-name type]
     (call-es es :head (make-url (multi index-name)
                                 (multi type))))
   good-or-not-found-status))

(def delete-mapping
  (typical-call
   (fn delete-mapping*
     ([es]
      (delete-mapping* es "_all" "_all"))
     ([es index-name]
      (delete-mapping* es index-name "_all"))
     ([es index-name type]
      (call-es es :delete (make-url (multi index-name)
                                    "_mapping"
                                    (multi type)))))
   good-status))

;; Aliases

(def aliases
  "End-point for atomically applying multiple alias operations"
  (typical-call
   (fn [es body]
     (call-es es :post (make-url "_aliases")
              :body body))
   good-status))

(def add-alias
  (typical-call
   (fn add-alias*
     ([es alias-name]
      (add-alias* es "_all" alias-name))
     ([es index-name alias-name]
      (call-es es :put (make-url (multi index-name)
                                 "_alias"
                                 alias-name))))
   good-status))

(def delete-alias
  (typical-call
   (fn delete-alias*
     ([es]
      (delete-alias* es "_all" "_all"))
     ([es index-name]
      (delete-alias* es index-name "_all"))
     ([es index-name alias-name]
      (call-es es :delete (make-url (multi index-name)
                                    "_alias"
                                    (multi alias-name)))))
   good-status))

(def get-alias
  (typical-call
   (fn get-alias*
     ([es]
      (get-alias* es "_all" "_all"))
     ([es index-name]
      (get-alias* es index-name "_all"))
     ([es index-name alias-name]
      (call-es es :get (make-url (multi index-name)
                                 "_alias"
                                 (multi alias-name)))))
   good-status))

(def update-settings
  (typical-call
   (fn [es index-name settings]
     (call-es es :put (make-url index-name "_settings")
              :body settings))
   good-status))

(def get-settings
  (typical-call
   (fn get-settings*
     ([es]
      (get-settings* es "_all"))
     ([es index-name]
      (call-es es :get (make-url (multi index-name)))))
   good-status))

;; Analyze

(def analyze
  (typical-call
   (fn analyze*
     ([es options test]
      (analyze* es nil options test))
     ([es index-name options test]
      (call-es es :get (make-url index-name
                                 "_analyze")
               :params options
               :body test)))
   good-status))

;; Templates

(def put-template
  (typical-call
   (fn [es template-name body]
     (call-es es :put (make-url "_template"
                                template-name)
              :body body))
   good-status))

(def delete-template
  (typical-call
   (fn [es template-name]
     (call-es es :delete (make-url "_template" template-name)))
   good-status))

(def get-template
  (typical-call
   (fn get-template*
     ([es]
      (get-template* es nil))
     ([es template-name]
      (call-es es :get (make-url "_template"
                                 (multi template-name)))))
   good-status))

(def template-exists
  (typical-call
   (fn [es template-name]
     (call-es es :head (make-url "_template" template-name)))
   good-status))

;; Warmers

(def put-warmer
  (typical-call
   (fn put-warmer*
     ([es warmer-name warmer]
      (put-warmer* es "_all" nil warmer-name warmer))
     ([es index-name warmer-name warmer]
      (put-warmer* es index-name nil warmer-name warmer))
     ([es index-name type warmer-name warmer]
      (call-es es :put (make-url (multi index-name)
                                 (multi type)
                                 "_warmer"
                                 warmer-name)
               :body warmer)))
   good-status))

(def delete-warmer
  (typical-call
   (fn delete-warmer*
     ([es]
      (delete-warmer* es "_all" "_all"))
     ([es index-name]
      (delete-warmer* es index-name "_all"))
     ([es index-name warmer-name]
      (call-es es :delete (make-url (multi index-name)
                                    "_warmer"
                                    (multi warmer-name)))))
   good-status))

(def get-warmer
  (typical-call
   (fn get-warmer*
     ([es]
      (get-warmer* "_all" nil))
     ([es index-name]
      (get-warmer* index-name nil))
     ([es index-name warmer-name]
      (call-es es :get (make-url (multi index-name)
                                 "_warmer"
                                 (multi warmer-name)))))
   good-status))

;; Stats

(def index-stats
  (typical-call
   (fn [es & {:keys [index-name stats] :as opts}]
     (let [opts (dissoc opts :index-name :stats)]
       (call-es es :get (make-url (multi index-name)
                                  "_stats"
                                  (multi stats))
                :params opts)))
   good-status))

(def index-segments
  (typical-call
   (fn index-segments*
     ([es]
      (index-segments* es nil))
     ([es index-name]
      (call-es es :get (make-url (multi index-name)
                                 "_segments"))))
   good-status))

(def index-recovery
  (typical-call
   (fn index-recovery*
     ([es]
      (index-recovery* es nil))
     ([es index-name]
      (call-es es :get (make-url (multi index-name)
                                 "_recovery"))))
   good-status))

(def clear-cache
  (typical-call
   (fn clear-cache*
     ([es]
      (clear-cache* es nil))
     ([es index-name]
      (call-es es :get (make-url (multi index-name)
                                 "_cache"
                                 "clear"))))
   good-status))

;; Flush

(def flush
  (typical-call
   (fn flush*
     ([es]
      (flush* es nil))
     ([es index-name]
      (call-es es :post (make-url (multi index-name)
                                  "_flush"))))
   good-status))

(def refresh
  (typical-call
   (fn refresh*
     ([es]
      (refresh* es nil))
     ([es index-name]
      (call-es es :post (make-url (multi index-name)
                                  "_refresh"))))
   good-status))

(def optimize
  (typical-call
   (fn optimize*
     ([es]
      (optimize* es nil))
     ([es index-name]
      (call-es es :post (make-url (multi index-name)
                                  "_optimize"))))
   good-status))

(def upgrade
  (typical-call
   (fn [es index-name]
     (call-es es :post (make-url (multi index-name)
                                 "_upgrade")))
   good-status))
