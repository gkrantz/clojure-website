(ns tower-defence.player-api
  (:require [tower-defence.definitions.towers]
            [tower-defence.definitions.monsters]
            [tower-defence.definitions.waves]
            [tower-defence.core :refer [add-monster-to-board
                                        add-waypoints-to-state
                                        all-towers-attempt-to-shoot
                                        attempt-to-spawn-monsters
                                        build-tower
                                        can-build-tower?
                                        check-if-phase-over
                                        create-game
                                        move-all-monsters
                                        remove-dead-monsters
                                        start-monster-phase]]
            [tower-defence.helpers :refer [create-monster
                                           create-tower]]))

(defn start-game
  []
  (-> (create-game {;:height 1
                    ;:width 1
                    :towers   {"t00" (create-tower "Basic" [11 1] :id "t00")
                               "t01" (create-tower "Basic" [10 1] :id "t01")
                               "t02" (create-tower "Basic" [8 0] :id "t02")
                               "t03" (create-tower "Basic" [8 1] :id "t03")
                               "t04" (create-tower "Basic" [8 2] :id "t04")
                               "t05" (create-tower "Basic" [8 3] :id "t05")
                               "t06" (create-tower "Basic" [9 3] :id "t06")
                               "t07" (create-tower "Basic" [10 3] :id "t07")}
                    :monsters {"m0" (create-monster "Blob" :id "m0" :y 336.0 :x 16.0 :target-wpt-idx 0)}})
      (add-waypoints-to-state)))

(defn attempt-build-tower
  [state name y x]
  (if (can-build-tower? state name [y x])
    (build-tower state name [y x])
    state))

(defn start-monster-wave
  [state]
  (start-monster-phase state))

(defn tick
  "A game tick during the monster phase."
  [state]
  (as-> (update state :current-tick inc) $
        (if (= (:phase state) :monster)
          (-> $
              (all-towers-attempt-to-shoot)
              (remove-dead-monsters)
              (move-all-monsters)
              (attempt-to-spawn-monsters)
              (check-if-phase-over))
          $)))