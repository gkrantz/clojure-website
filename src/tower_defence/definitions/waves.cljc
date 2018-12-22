(ns tower-defence.definitions.waves
  (:require [tower-defence.definitions :as definitions]))

(def wave-definitions
  {"wave 1" {"20 blobs" {:count    20
                         :name     "Blob"
                         :interval 1000}}})

(definitions/add-definitions! wave-definitions)
