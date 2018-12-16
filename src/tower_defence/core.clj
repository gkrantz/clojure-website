(ns tower-defence.core
  (:require [clojure.test :refer [is]]
            [tower-defence.definitions :refer [get-definition]]
            [tower-defence.pathfinding :refer [find-path]]
            [tower-defence.helpers :refer [force-add-tower
                                           generate-id
                                           get-end
                                           get-gold
                                           get-height
                                           get-start
                                           get-tower-cost
                                           get-tower-locations
                                           get-width
                                           reduce-gold]]))

(defn create-empty-state
  []
  {:towers      {}
   :monsters    {}
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

(defn create-tower
  "Creates a tower given a name."
  {:test (fn []
           (is (= (create-tower "Basic")
                  {:name     "Basic"
                   :fired-at 0
                   :dir      0}))
           (is (= (create-tower "Basic" :y 1 :x 1)
                  {:name     "Basic"
                   :fired-at 0
                   :dir      0
                   :y        1
                   :x        1})))}
  [name & kvs]
  (let [tower {:name     name
               :fired-at 0
               :dir      0}]
    (if (empty? kvs)
      tower
      (apply assoc tower kvs))))

(defn build-tower
  "Builds a tower without checks."
  {:test (fn [] (as-> (create-empty-state) $
                      (build-tower $ "Basic" [1 1])
                      (do (is (= (:towers $) {[1 1] (create-tower "Basic" :id "t1" :x 1 :y 1)}))
                          (is (= (get-gold $) 90)))))}
  [state name [y x]]
  (-> (reduce-gold state (get-tower-cost name))
      (force-add-tower (create-tower name :id (generate-id "t") :y y :x x) [y x])))