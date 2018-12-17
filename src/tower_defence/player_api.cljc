(ns tower-defence.player-api
  (:require [tower-defence.core :refer [create-game
                                        create-tower]]))

(defn start-game
  []
  (create-game {:towers {"t0" (create-tower "Basic" [5 5] :id "t0")}}))