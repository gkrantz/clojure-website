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
                                        update-all-projectiles
                                        remove-dead-monsters
                                        start-monster-phase]]
            [tower-defence.helpers :refer [create-monster
                                           create-tower
                                           is-monster-phase?]]))

(defn start-game
  []
  (-> (create-game {:towers   {"t00" (create-tower "Basic" [1 11] :id "t00")
                               "t01" (create-tower "Basic" [1 10] :id "t01")
                               "t02" (create-tower "Basic" [0 8] :id "t02")
                               "t03" (create-tower "Basic" [1 8] :id "t03")
                               "t04" (create-tower "Basic" [2 8] :id "t04")
                               "t05" (create-tower "Basic" [3 8] :id "t05")
                               "t06" (create-tower "Basic" [3 9] :id "t06")
                               "t07" (create-tower "Basic" [3 10] :id "t07")}
                    :monsters {"m0" (create-monster "Blob" :id "m0" :x 16.0 :y 336.0 :target-wpt-idx 0)}
                    :gold 99999})
      (add-waypoints-to-state)))

(defn attempt-build-tower
  [state name x y]
  (if (can-build-tower? state name [x y])
    (build-tower state name [x y])
    state))

(defn start-monster-wave
  [state]
  (start-monster-phase state))

(defn tick
  [state]
  (as-> (update state :current-tick inc) $
        (if (is-monster-phase? state)
          (-> $
              (all-towers-attempt-to-shoot)
              (update-all-projectiles)
              (remove-dead-monsters)
              (move-all-monsters)
              (attempt-to-spawn-monsters)
              (check-if-phase-over))
          $)))