(ns tower-defence.player-api
  (:require [tower-defence.definitions.towers]
            [tower-defence.definitions.monsters]
            [tower-defence.core :refer [create-game
                                        all-towers-attempt-to-shoot
                                        remove-dead-monsters
                                        move-all-monsters
                                        add-waypoints-to-state]]
            [tower-defence.helpers :refer [create-monster
                                           create-tower]]))

(defn start-game
  []
  (-> (create-game {;:height 1
                    ;:width 1
                    :towers   {"t0" (create-tower "Basic" [5 5] :id "t0")}
                    :monsters {"m0" (create-monster "Blob" :id "m0" :y 16.0 :x 16.0 :target-wpt-idx 0)}})
      (add-waypoints-to-state)))

(defn tick
  "A game tick during the monster phase."
  [state]
  (-> (all-towers-attempt-to-shoot state)
      (remove-dead-monsters)
      (move-all-monsters)))