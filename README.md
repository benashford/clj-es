# clj-es

An asynchronous ElasticSearch client for Clojure.

Currently under development.  Warning: more APIs have been implemented than tested at this stage.

[![Clojars Project](http://clojars.org/clj-es/latest-version.svg)](http://clojars.org/clj-es)

Tested with ElasticSearch 1.6.

[![Build Status](https://travis-ci.org/benashford/clj-es.svg?branch=master)](https://travis-ci.org/benashford/clj-es)

## Overview

This project was intended to complement my [asynchronous Redis client](https://github.com/benashford/redis-async), and to contrast my [ElasticSearch client for Rust](https://github.com/benashford/rs-es).

The goal is to provide a very lightweight client that doesn't hide or obscure any of the advanced features of ElasticSearch, and to have a direct mapping to the raw APIs so that users familiar with the ElasticSearch APIs will be able to use this client directly.

`clj-es.client` contains one function per API.  The first parameter is always the "client" (a map containing hostname and/or port, these will default to `localhost` and `9200` if not set), the next parameters correspond to the path elements of each ES call.  The final (optional) parameter, is the body (e.g. the document or query). For example:

```clojure
(index es-client "index-name" "doc-type" document)
```

Will translate as a POST to `http://es:9200/index-name/doc-type` with the document as the body.

Other parameters that are sent as query parameters, e.g. routing and version fields, are set using `with-params`:

```clojure
(with-params {:version 5}
  (index es-client "index-name" "doc-type" id document))
```

### Queries

Many ElasticSearch operations take use queries.  These are either in the form of a [URI query parameter](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-uri-request.html) or in the body (along side other arguments) in the form of the [query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html).

Functions that use queries support both of these options.  If a `String` is provided, it will be used as a URI search; otherwise it is assumed to be a request body that will contain a query.

### Multi-index, multi-type

Many ElasticSearch operations can be applied to multiple indexes and multiple types.  For these operations either a `String`, representing a single index/type; or a sequence or vector of `String`s, representing multiple indexes/types, can be provided.

## APIs supported

### Document APIs

#### `index`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html

#### `get`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html

#### `delete`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html

#### `update`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html

#### `multi-get`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html

#### `bulk`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html

#### `delete-by-query`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html

Please note, this is scheduled to be removed in ElasticSearch 2.0

#### `termvectors`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-termvectors.html

#### `multi-termvectors`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-termvectors.html

### Search APIs

#### `search`

Supports both search by URI: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-uri-request.html, and search by query body: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html.

Because this is a thin layer, anything that can be expressed via a request-body search is supported here - including aggregations and scan/scroll operations.

#### `template-search`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html

Templates can be got and set using `search-template`

#### `search-shards`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-shards.html

#### `suggest`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html

#### `multi-search`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-multi-search.html

#### `count`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-count.html

#### `search-exists`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-exists.html

#### `validate`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-validate.html

#### `explain`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-explain.html

#### `percolate`, `percolate-count`, and `multi-percolate`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-percolate.html

#### `more-like-this`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/search-more-like-this.html

### Index APIs

#### `create-index`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html

#### `delete-index`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html

#### `get-index`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-index.html

#### `index-exists`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-exists.html

#### `open-index` and `close-index`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html

#### `put-mapping`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html

#### `get-mapping`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html and https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-field-mapping.html

#### `type-exists`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-types-exists.html

#### `delete-mapping`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-mapping.html

#### `aliases`, `add-alias`, `delete-alias` and `get-alias`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-aliases.html

#### `update-settings`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-update-settings.html

#### `get-settings`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-settings.html

#### `analyze`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-analyze.html

#### `put-template`, `delete-template`, `get-template` and `template-exists`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-templates.html

#### `put-warmer`, `delete-warmer` and `get-warmer`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-warmers.html

#### `index-stats`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html

#### `index-segments`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-segments.html

#### `index-recovery`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-recovery.html

#### `clear-cache`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-clearcache.html

#### `flush`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-flush.html

#### `refresh`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html

#### `optimize`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-optimize.html

#### `upgrade`

See: https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-upgrade.html

### Cat APIs

Not yet supported

### Cluster APIs

Not yet supported

## Implementation details

A pool of persistent HTTP connections are used, with a default keepalive value of 30 seconds.

## Why not use...

TBC: compare with existing clients

## TODO

1. Release to Clojars
2. Implementation of Field stats API (https://www.elastic.co/guide/en/elasticsearch/reference/current/search-more-like-this.html)
3. Helper functions for scan/scroll
4. Helper functions for bulk

## License

```
Copyright 2015 Ben Ashford

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
