(ns tower-defence.game.definitions.towers
  (:require [tower-defence.game.definitions :as definitions]))

(def tower-definitions
  {"Pea Shooter" {:name       "Pea Shooter"
                  :cost       10
                  :range      64
                  :rate       1500
                  :projectile {:damage 10
                               :speed  60}}
   "MGT-MK1"     {:name       "MGT-MK1"
                  :cost       50
                  :range      100
                  :rate       100
                  :projectile {:damage 3
                               :speed  240}}})

(definitions/add-definitions! tower-definitions)