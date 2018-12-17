(ns tower-defence.player-api
  (:require [tower-defence.core :refer [create-game
                                        create-tower
                                        all-towers-attempt-to-shoot
                                        remove-dead-monsters
                                        move-all-monsters]]))

(defn start-game
  []
  (create-game {:towers {"t0" (create-tower "Basic" [5 5] :id "t0")}}))

(defn tick
  "A game tick during the monster phase."
  [state]
  (-> (all-towers-attempt-to-shoot state)
      (remove-dead-monsters)
      (move-all-monsters)))