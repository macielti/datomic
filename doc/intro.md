# Introduction to `net.clojars.macielti/datomic`

## What is this?

`net.clojars.macielti/datomic` is a thin Clojure library that wraps a [Datomic Peer](https://docs.datomic.com/peer/index.html) connection as an [Integrant](https://github.com/weavejester/integrant) component. It handles:

- Creating and connecting to a Datomic database on system start
- Transacting your schemas automatically on startup
- Retrying the connection up to 3 times on failure (via [Diehard](https://github.com/sunng87/diehard))
- Releasing the connection gracefully on system halt
- A `mocked-datomic` helper for isolated in-memory connections in tests

---

## Installation

Add the dependency to your `project.clj`:

```clojure
[net.clojars.macielti/datomic "1.0.0"]
```

Or with `deps.edn`:

```clojure
net.clojars.macielti/datomic {:mvn/version "1.0.0"}
```

---

## Configuration

The component is identified by the key `::datomic.component/datomic` in your Integrant config map.

```clojure
{:datomic.component/datomic {:schemas    [...]
                              :components {:config {:datomic-uri "datomic:mem://mydb"}}}}
```

### Keys

| Key | Required | Description |
|-----|----------|-------------|
| `:schemas` | yes | Vector of Datomic attribute maps to transact on startup |
| `:components :config :datomic-uri` | no | Datomic URI string. Defaults to a fresh `datomic:mem://<random-uuid>` if omitted |

### Schema definition example

Each entry in `:schemas` is a standard Datomic attribute map:

```clojure
[{:db/ident       :example/id
  :db/valueType   :db.type/uuid
  :db/cardinality :db.cardinality/one
  :db/unique      :db.unique/identity
  :db/doc         "Example entity ID"}
 {:db/ident       :example/description
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one
  :db/doc         "Human-readable description"}]
```

---

## Starting and stopping the system

Use the standard Integrant lifecycle:

```clojure
(require '[integrant.core :as ig]
         '[datomic.component])

(def config
  {:datomic.component/datomic
   {:schemas    [{:db/ident       :example/id
                  :db/valueType   :db.type/uuid
                  :db/cardinality :db.cardinality/one
                  :db/unique      :db.unique/identity}]
    :components {:config {:datomic-uri "datomic:mem://myapp"}}}})

;; Start
(def system (ig/init config))

;; Stop
(ig/halt! system)
```

On `ig/init`, the component:
1. Creates the database at the given URI (or an in-memory one)
2. Connects to it (with up to 3 retries)
3. Transacts all provided schemas
4. Returns the `LocalConnection` object

On `ig/halt!`, it calls `d/release` on the connection.

---

## Using the connection

After `ig/init`, retrieve the connection from the system map using the component key:

```clojure
(require '[datomic.api :as d])

(def connection (:datomic.component/datomic system))

;; Transact data
@(d/transact connection [{:example/id          (random-uuid)
                           :example/description "Hello, Datomic!"}])

;; Query
(d/q '[:find ?desc
       :where [_ :example/description ?desc]]
     (d/db connection))
```

---

## `transact-and-lookup-entity!`

A convenience helper that transacts a single entity and immediately pulls it back from `db-after`.

### Signature

```clojure
(transact-and-lookup-entity! identity-key entity connection)
```

| Argument | Type | Description |
|----------|------|-------------|
| `identity-key` | Keyword | The attribute used to look the entity back up (must be `:db/unique`) |
| `entity` | Map | The entity map to transact |
| `connection` | `LocalConnection` | Active Datomic connection |

### Return value

```clojure
{:entity   {...}   ; the full entity map (excluding :db/id)
 :db-after <Db>}   ; the database value after the transaction
```

### Example

```clojure
(require '[datomic.component :as component.datomic])

(def result
  (component.datomic/transact-and-lookup-entity!
    :example/id
    {:example/id          (random-uuid)
     :example/description "Transacted and retrieved"}
    connection))

(:entity result)
;; => {:example/id #uuid "...", :example/description "Transacted and retrieved"}
```

Throws `ex-info` if the entity cannot be found after transacting.

---

## Testing with `mocked-datomic`

For unit tests you can create an isolated in-memory connection without Integrant:

```clojure
(require '[datomic.component :as component.datomic]
         '[datomic.api :as d])

(def schema
  [{:db/ident       :example/id
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}
   {:db/ident       :example/description
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(deftest my-db-test
  (let [connection (component.datomic/mocked-datomic schema)]
    (is (some? @(d/transact connection [{:example/id          (random-uuid)
                                         :example/description "test"}])))))
```

`mocked-datomic` creates a fresh `datomic:mem://<uuid>` database, connects to it, transacts the schemas, and returns the connection. Each call produces a fully isolated database.
