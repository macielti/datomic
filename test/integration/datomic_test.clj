(ns datomic-test
  (:require [clojure.test :refer [is testing]]
            [datomic.api :as d]
            [datomic.component :as component.datomic]
            [integrant.core :as ig]
            [matcher-combinators.test :refer [match?]]
            [schema.test :as s])
  (:import (datomic.db Db)
           (datomic.peer LocalConnection)))

(def minimal-schema-for-test
  [{:db/ident       :example/id
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Example ID"}
   {:db/ident       :example/description
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Example description"}])

(def entity {:example/id          (random-uuid)
             :example/description "Não é impossível ser feliz depois que agente cresce… só é mais complicado."})

(s/deftest mocked-datomic-test
  (testing "Should be able to create a temporary and isolated in-memory datomic connection to support unit test for database operations"
    (let [connection (component.datomic/mocked-datomic minimal-schema-for-test)]
      (is (match? {:tx-data seqable?}
                  @(d/transact connection [entity]))))))

(def config {::component.datomic/datomic {:schemas    minimal-schema-for-test
                                          :components {:config {:datomic-uri (str "datomic:mem://" (random-uuid))}}}})

(s/deftest datomic-integrant-component-test
  (let [system (ig/init config)
        connection (:datomic.component/datomic system)]
    (testing "Should be able to init a system with datomic component"
      (is (match? {:datomic.component/datomic #(= (type %) LocalConnection)}
                  system)))
    (testing "Should be able to use the initiated datomic component to perform database operations"
      (is (match? {:tx-data seqable?}
                  @(d/transact connection [entity])))

      (is (match? {:db-after #(= (type %) Db)
                   :entity   entity}
                  (component.datomic/transact-and-lookup-entity! :example/id entity connection))))
    (testing "The System was stopped"
      (is (nil? (ig/halt! system))))))
