(ns tower-defence.definitions.monsters
  (:require [tower-defence.definitions :as definitions]))

(def monster-definitions
  {"Blob" {:name   "Blob"
           :health 15
           :speed  30}})

(definitions/add-definitions! monster-definitions)
