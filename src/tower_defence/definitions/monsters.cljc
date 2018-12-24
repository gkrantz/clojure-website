(ns tower-defence.definitions.monsters
  (:require [tower-defence.definitions :as definitions]))

(def monster-definitions
  {"Blob"   {:name   "Blob"
             :health 100
             :speed  30}
   "Spider" {:name   "Spider"
             :health 1000
             :speed  15}})

(definitions/add-definitions! monster-definitions)
