# Datomic Component

An [Integrant](https://github.com/weavejester/integrant) component that wraps a [Datomic Peer](https://docs.datomic.com/peer/index.html) connection. It creates the database, transacts your schemas, and manages the connection lifecycle automatically — with built-in retry logic on startup.

## Installation

**project.clj**
```clojure
[net.clojars.macielti/datomic "1.0.0"]
```

**deps.edn**
```clojure
net.clojars.macielti/datomic {:mvn/version "1.0.0"}
```

## Quick start

```clojure
(require '[integrant.core :as ig]
         '[datomic.component]
         '[datomic.api :as d])

(def config
  {:datomic.component/datomic
   {:schemas    [{:db/ident       :user/id
                  :db/valueType   :db.type/uuid
                  :db/cardinality :db.cardinality/one
                  :db/unique      :db.unique/identity}
                 {:db/ident       :user/name
                  :db/valueType   :db.type/string
                  :db/cardinality :db.cardinality/one}]
    :components {:config {:datomic-uri "datomic:mem://myapp"}}}})

;; Start the system — creates the DB, connects, and transacts schemas
(def system (ig/init config))

;; Get the connection
(def conn (:datomic.component/datomic system))

;; Use it
@(d/transact conn [{:user/id (random-uuid) :user/name "Alice"}])

;; Stop the system
(ig/halt! system)
```

Omit `:datomic-uri` and the component will create a temporary in-memory database automatically.

## Documentation

See [doc/intro.md](doc/intro.md) for the full guide, including:

- Configuration reference
- Schema definition examples
- `transact-and-lookup-entity!` helper
- Testing with `mocked-datomic`

## License

Copyright © 2024

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.
