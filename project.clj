(defproject clj-es "0.1.0"
  :description "Lightweight async client for the ElasticSearch REST API"
  :url "https://github.com/benashford/clj-es"
  :license {:name         "Apache Licence 2.0"
            :url          "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cheshire "5.5.0"]
                 [http-kit "2.1.19"]])
