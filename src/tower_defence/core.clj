(ns tower-defence.core
  (:require [clojure.test :refer [is]]
            [tower-defence.definitions :refer [get-definition]]
            [tower-defence.pathfinding :refer [find-path]]
            [tower-defence.getters-and-setters :refer [get-end
                                                       get-gold
                                                       get-height
                                                       get-start
                                                       get-tower-cost
                                                       get-tower-locations
                                                       get-width]]))

(defn create-empty-state
  []
  {:towers      {}
   :monsters    []
   :projectiles []
   :height      12
   :width       12
   :start       [11 0]
   :end         [0 11]
   :gold        100
   :lives       10})

(defn create-game
  [data]
  (merge (create-empty-state) (or data {})))

(defn force-add-tower
  "Adds a tower to the state without any checking if it's healthy for the state."
  [state tower [y x]]
  (assoc-in state [:towers [y x]] tower))

(defn calculate-monster-path
  "Calculates the path the monsters will take in a state."
  {:test (fn []
           (is (= (-> (create-game {:height 1
                                    :width  3
                                    :start  [0 0]
                                    :end    [0 2]})
                      (force-add-tower "blocking?" [0 1])
                      (calculate-monster-path))
                  nil))
           (is (= (-> (create-game {:height 1
                                    :width  3
                                    :start  [0 0]
                                    :end    [0 2]})
                      (calculate-monster-path))
                  [[0 0] [0 1] [0 2]])))}
  [state]
  (as-> (reduce (fn [unwalkables tower-location]
                  (assoc unwalkables tower-location :unwalkable))
                {}
                (get-tower-locations state)) $
        (find-path (get-height state)
                   (get-width state)
                   $)))

(defn can-build-tower?
  {:test (fn []
           (is (not (-> (create-game {:height 1
                                      :width  3
                                      :start  [0 0]
                                      :end    [0 2]})
                        (can-build-tower? "Basic" [0 1]))))

           (is (-> (create-game {:height 2
                                 :width  3
                                 :start  [0 0]
                                 :end    [0 2]})
                   (can-build-tower? "Basic" [0 1]))))}
  [state name [y x]]
  (and (not (= [y x] (get-start state)))
       (not (= [y x] (get-end state)))
       (not (nil? (calculate-monster-path (force-add-tower state "blocking?" [y x]))))
       (>= (get-gold state) (get-tower-cost name))))