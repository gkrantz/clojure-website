(ns tower-defence.game.definitions
  (:require [tower-defence.game.definitions.towers :refer [tower-definitions
                                                           projectile-definitions]]
            [tower-defence.game.definitions.waves :refer [wave-definitions]]
            [tower-defence.game.definitions.monsters :refer [monster-definitions]]))

(def all-definitions
  (merge tower-definitions
         wave-definitions
         monster-definitions
         projectile-definitions))

(defn get-definition
  [name-or-map]
  (if (map? name-or-map)
    (get-definition (:name name-or-map))
    (as-> (-> (get all-definitions name-or-map)) $
          (do (when (nil? $)
                (println (str "Missing definition: " name-or-map ".")))
              $))))