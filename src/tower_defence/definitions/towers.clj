(ns tower-defence.definitions.towers
  (:require [tower-defence.definitions :as definitions]))

(def tower-definitions
  {"Basic" {:name "Basic"
            :cost 10}})

(definitions/add-definitions! tower-definitions)