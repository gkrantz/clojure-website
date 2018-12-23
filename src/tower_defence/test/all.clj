(ns tower-defence.test.all
  (:require [clojure.test :refer [is successful? run-tests deftest]]
            [tower-defence.core]
            [tower-defence.helpers]
            [tower-defence.pathfinding]))

(deftest test-all
         (let [namespaces (->> (all-ns)
                               (map str)
                               (filter (fn [x] (re-matches #"tower-defence\..*" x)))
                               (remove (fn [x] (= "tower-defence.test.all" x)))
                               (map symbol))]
           (is (successful? (time (apply run-tests namespaces))))))