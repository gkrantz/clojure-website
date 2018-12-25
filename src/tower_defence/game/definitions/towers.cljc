(ns tower-defence.game.definitions.towers
  (:require [tower-defence.game.definitions :as definitions]))

(def tower-definitions
  {"Pea Shooter" {:name       "Pea Shooter"
                  :cost       10
                  :range      64
                  :rate       1500
                  :projectile {:damage 10
                               :speed  60}}})

(definitions/add-definitions! tower-definitions)