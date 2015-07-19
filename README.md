# clj-es

An asynchronous ElasticSearch client for Clojure.

Currently under development.  Warning: more APIs have been implemented than tested at this stage.

Will be published to Clojars in due course once required stability has been achieved.

Tested with ElasticSearch 1.6.

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

## APIs supported

### `index`



## Why not use...

TBC: compare with existing clients

## TODO

1. Testing (including link on Travis)
2. Documentation
3. Higher-level extras (e.g. helper functions for scan/scroll)
