(ns tower-defence.test.all
  (:require [clojure.test :refer [is successful? run-tests deftest]]
            [tower-defence.game.core]
            [tower-defence.game.helpers]
            [tower-defence.game.pathfinding]
            [tower-defence.game.definitions]
            [tower-defence.game.definitions.monsters]
            [tower-defence.game.definitions.towers]
            [tower-defence.game.definitions.waves]))

(deftest test-all
         (let [namespaces (->> (all-ns)
                               (map str)
                               (filter (fn [x] (re-matches #"tower-defence\..*" x)))
                               (remove (fn [x] (= "tower-defence.test.all" x)))
                               (map symbol))]
           (is (successful? (time (apply run-tests namespaces))))))