(ns tower-defence.game.definitions
  (:require [tower-defence.game.definitions.towers :refer [tower-definitions]]
            [tower-defence.game.definitions.waves :refer [wave-definitions]]
            [tower-defence.game.definitions.monsters :refer [monster-definitions]]))

(def all-definitions
  (merge tower-definitions
         wave-definitions
         monster-definitions))

(defn get-definition
  [name]
  (as-> (-> (get all-definitions name)) $
        (do (when (nil? $)
              (println (str "Missing definition: " name ".")))
            $)))