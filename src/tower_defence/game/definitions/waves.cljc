(ns tower-defence.game.definitions.waves)

(def wave-definitions
  {"wave 0" {:classes {"20 blobs"  {:count    20
                                    :name     "Blob"
                                    :interval 1000}
                       "2 spiders" {:count    2
                                    :name     "Spider"
                                    :interval 10000}}
             :next    "wave 1"}
   "wave 1" {:classes {"10 bugs" {:count    10
                                   :name     "Bug"
                                   :interval 500}
                       "3 bugs"  {:count    3
                                   :name     "Bug"
                                   :interval 1667}}
             :next    "wave 2"}
   "wave 2" {:classes {"20 spiders" {:count    20
                                     :name     "Spider"
                                     :interval 500}}
             :next    "none"}})