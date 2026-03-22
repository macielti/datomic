(ns datomic.schema
  (:require [schema.core :as s])
  (:import (datomic.db Db)))

(def DatomicAttribute
  {s/Keyword s/Any})

(def DatomicSchemas
  [DatomicAttribute])

(def TransactAndLookupEntityResult
  {:entity   (s/pred map?)
   :db-after Db})
