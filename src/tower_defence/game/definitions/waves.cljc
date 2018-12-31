(ns tower-defence.game.definitions.waves)

(def wave-definitions
  {"wave 0" {:classes {"20 rats" {:count    20
                                  :name     "Rat"
                                  :interval 1000}
                       "2 blobs" {:count    2
                                  :name     "Blob"
                                  :interval 10000}}
             :next    "wave 1"}
   "wave 1" {:classes {"10 bugs"   {:count    10
                                    :name     "Bug"
                                    :interval 500}
                       "3 spiders" {:count    3
                                    :name     "Spider"
                                    :interval 2500}}
             :next    "wave 2"}
   "wave 2" {:classes {"20 spiders" {:count    20
                                     :name     "Spider"
                                     :interval 500}}
             :next    "wave 3"}
   "wave 3" {:classes {"1 war turtle" {:count    1
                                       :name     "War Turtle"
                                       :interval 1000}}
             :next    "wave 0"}})