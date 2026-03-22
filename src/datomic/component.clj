(ns datomic.component
  (:require [datomic.api :as d]
            [datomic.schema :as datomic.schema]
            [diehard.core :as dh]
            [integrant.core :as ig]
            [schema.core :as s]
            [taoensso.timbre :as log])
  (:import (datomic.peer LocalConnection)))

(s/defn mocked-datomic :- LocalConnection
  [datomic-schemas :- datomic.schema/DatomicSchemas]
  (let [datomic-uri (str "datomic:mem://" (random-uuid))
        connection (do (d/create-database datomic-uri)
                       (d/connect datomic-uri))]
    @(d/transact connection (flatten datomic-schemas))
    connection))

(s/defn transact-and-lookup-entity! :- datomic.schema/TransactAndLookupEntityResult
  [identity-key :- s/Keyword
   entity :- (s/pred map?)
   connection :- LocalConnection]
  (let [{:keys [db-after]} @(d/transact connection [entity])
        entity-identity-id (identity-key entity)
        query-result (d/q '[:find (pull ?entity [*])
                            :in $ ?identity-key ?entity-identity-id
                            :where [?entity ?identity-key ?entity-identity-id]]
                          db-after identity-key entity-identity-id)
        entity' (-> query-result ffirst (dissoc :db/id))]
    (when-not entity'
      (throw (ex-info "Entity not found after transacting it" {:entity entity})))
    {:entity   entity'
     :db-after db-after}))

(defmethod ig/init-key ::datomic
  [_ {:keys [components schemas]}]
  (log/info :starting ::datomic)
  (let [datomic-uri (or (-> components :config :datomic-uri)
                        (str "datomic:mem://" (random-uuid)))
        connection (dh/with-retry {:retry-on    Exception
                                   :max-retries 3}
                     (log/info ::database-created? (d/create-database datomic-uri))
                     (d/connect datomic-uri))]
    @(d/transact connection (flatten schemas))
    connection))

(defmethod ig/halt-key! ::datomic
  [_ datomic]
  (log/info :stopping ::datomic)
  (d/release datomic))
