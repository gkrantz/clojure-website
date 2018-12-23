(ns tower-defence.definitions.towers
  (:require [tower-defence.definitions :as definitions]))

(def tower-definitions
  {"Basic" {:name       "Basic"
            :cost       10
            :range      64
            :rate       15
            :projectile {:damage 10
                         :speed  60}}})

(definitions/add-definitions! tower-definitions)