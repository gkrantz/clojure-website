(ns tower-defence.definitions.waves
  (:require [tower-defence.definitions :as definitions]))

(def wave-definitions
  {"wave 0" {:classes {"20 blobs"  {:count    20
                                    :name     "Blob"
                                    :interval 1000}
                       "2 spiders" {:count    2
                                    :name     "Giant Spider"
                                    :interval 10000}}
             :next    "wave 1"}
   "wave 1" {:classes {"10 blobs" {:count    10
                                   :name     "Blob"
                                   :interval 500}
                       "3 blobs"  {:count    3
                                   :name     "Blob"
                                   :interval 1667}}
             :next    "none"}})

(definitions/add-definitions! wave-definitions)
