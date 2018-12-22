(ns tower-defence.core
  (:require [clojure.test :refer [is]]
            [tower-defence.definitions :refer [get-definition]]
            [tower-defence.pathfinding :refer [find-path]]
            [tower-defence.helpers :refer [calculate-angle
                                           calculate-middle-of-square
                                           create-monster
                                           create-tower
                                           damage-monster
                                           distance
                                           force-add-monster
                                           force-add-tower
                                           generate-id
                                           get-abs-start
                                           get-angle
                                           get-damage
                                           get-end
                                           get-gold
                                           get-height
                                           get-monster
                                           get-monsters
                                           get-monster-ids
                                           get-monster-wpt
                                           get-tower
                                           get-towers
                                           get-range
                                           get-rate
                                           get-speed
                                           get-start
                                           get-tower-cost
                                           get-tower-locations
                                           get-width
                                           get-x
                                           get-y
                                           is-dead?
                                           reduce-gold
                                           update-monster
                                           update-tower]]
            [tower-defence.constants :refer [TICKS_PER_SECOND]]))

(defn create-empty-state
  []
  {:towers       {}
   :monsters     {}
   :projectiles  []
   :height       12
   :width        12
   :start        [11 0]
   :end          [0 11]
   :gold         100
   :lives        10
   :wave         1
   :phase        :build
   :level        :test
   :current-tick 0
   :counter      1
   :waypoints    {}})

(defn create-game
  ([]
   (create-game {}))
  ([data]
   (merge (create-empty-state) (or data {}))))

(defn calculate-monster-path
  "Calculates the path the monsters will take in a state."
  {:test (fn []
           (is (= (-> (create-game {:height 1
                                    :width  3
                                    :start  [0 0]
                                    :end    [0 2]})
                      (force-add-tower (create-tower "Basic" [0 1]))
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
                   $
                   (get-start state)
                   (get-end state))))

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
       (not (nil? (calculate-monster-path (force-add-tower state (create-tower "Basic" [y x])))))
       (>= (get-gold state) (get-tower-cost name))))

(defn build-tower
  "Builds a tower without checks."
  {:test (fn [] (as-> (create-empty-state) $
                      (build-tower $ "Basic" [1 1])
                      (do (is (= (get-gold $) 90)))))}
  [state name [y x]]
  (as-> (reduce-gold state (get-tower-cost name)) $
        (let [[new_state id] (generate-id $ "t")]
          (force-add-tower new_state (create-tower name [y x] :id id)))))

(defn- create-waypoint
  [fromy fromx [toy tox]]
  {:x     (calculate-middle-of-square tox)
   :y     (calculate-middle-of-square toy)
   :angle (calculate-angle fromy fromx toy tox)})

(defn add-waypoints-to-state
  {:test (fn []
           (is (= (-> (create-empty-state)
                      (add-waypoints-to-state)
                      (:waypoints))
                  {0 {:angle (calculate-angle 1 0 0 1)
                      :x     (calculate-middle-of-square 11)
                      :y     (calculate-middle-of-square 0)}}))
           (is (= (-> (create-game {:start  [0 0]
                                    :end    [1 1]
                                    :towers {"b1" (create-tower "Basic" [0 1])}
                                    :width  2
                                    :height 2})
                      (add-waypoints-to-state)
                      (:waypoints))
                  {0 {:angle (calculate-angle 0 0 1 0)
                      :y     (calculate-middle-of-square 1)
                      :x     (calculate-middle-of-square 0)}
                   1 {:angle (calculate-angle 1 0 1 1)
                      :y     (calculate-middle-of-square 1)
                      :x     (calculate-middle-of-square 1)}})))}
  [state]
  (as-> (calculate-monster-path state) $
        (reduce (fn [[waypoints [fromy fromx] idx] node]
                  (let [wpt (create-waypoint fromy fromx node)]
                    (if (= (:angle (get waypoints (- idx 1)))
                           (:angle wpt))
                      [(assoc waypoints (- idx 1) wpt) node idx]
                      [(assoc waypoints idx wpt) node (+ idx 1)])))
                [{} (first $) 0]
                (drop 1 $))
        (assoc state :waypoints (first $))))

(defn add-monster-to-board
  ([state name]
   (let [[absy absx] (get-abs-start state)]
     (add-monster-to-board state name absy absx)))
  ([state name absy absx]
   (let [[new_state id] (generate-id state "m")]
     (as-> (create-monster name
                           :y absy
                           :x absx
                           :id id
                           :target-wpt-idx 0) $
           (force-add-monster new_state $)))))

(defn remove-monster
  {:test (fn [] (is (= (-> (create-game {:monsters {"m1" (create-monster "Blob" :id "m1")
                                                    "m2" (create-monster "Blob" :id "m2")}})
                           (remove-monster "m1")
                           (:monsters)
                           (keys))
                       ["m2"])))}
  [state id]
  (update state :monsters (fn [old] (dissoc old id))))

(defn monster-reached-end
  [state id]
  ;;TODO Remove lives etc.
  (remove-monster state id))

(defn next-waypoint
  "Increments the target waypoint for a monster."
  [state id]
  (if (= (:target-wpt-idx (get-monster state id)) (- (count (:waypoints state)) 1))
    (monster-reached-end state id)
    (update-monster state id (fn [m] (update m :target-wpt-idx inc)))))

(defn check-waypoint
  [state id]
  (let [monster (get-monster state id)
        wpt (get-monster-wpt state id)
        dy (Math/abs (- (:y monster) (:y wpt)))
        dx (Math/abs (- (:x monster) (:x wpt)))]
    (if (and (< dy 1)
             (< dx 1))
      (next-waypoint state id)
      state)))

(defn move-monster
  {:test (fn []
           (is (> (-> (create-game {:start     [0 0]
                                    :end       [1 1]
                                    :height    2
                                    :width     2
                                    :waypoints {0 {:angle (calculate-angle 0 0 1 0)
                                                   :y     (calculate-middle-of-square 1)
                                                   :x     (calculate-middle-of-square 0)}}})
                      (add-monster-to-board "Blob")
                      (move-monster "m1")
                      (get-y "m1"))
                  16.0)))}
  [state id]
  (let [speed (/ (get-speed state id) TICKS_PER_SECOND)
        angle (get-angle state id)
        dx (* speed (Math/cos angle))
        dy (* speed (Math/sin angle))]
    (-> (update-monster state id (fn [old] (update old :x + dx)))
        (update-monster id (fn [old] (update old :y + dy)))
        (update-monster id (fn [old] (assoc old :angle angle)))
        (check-waypoint id))))

(defn move-all-monsters
  [state]
  (reduce (fn [state_ id]
            (move-monster state_ id))
          state
          (get-monster-ids state)))

(defn find-target
  {:test (fn []
           (is (= (-> (find-target [(create-monster "Blob" :id "b1" :y 100 :x 100)
                                    (create-monster "Blob" :id "b2" :y 10 :x 10)] 15 15 10)
                      (:id))
                  "b2"))
           (is (= (find-target [(create-monster "Blob" :id "b1" :y 100 :x 100)] 15 15 10)
                  nil)))}
  [monsters y x range]
  (when (not (empty? monsters))
    (let [monster (first monsters)]
      (if (<= (distance y x (:y monster) (:x monster)) range)
        monster
        (find-target (drop 1 monsters) y x range)))))

(defn ready-to-shoot?
  {:test (fn []
           (is (not (-> (create-game {:current-tick 9})
                        (ready-to-shoot? (create-tower "Basic" [0 0])))))
           (is (-> (create-game {:current-tick 15})
                   (ready-to-shoot? (create-tower "Basic" [0 0])))))}
  [state tower]
  (<= (get-rate state tower) (- (:current-tick state) (:fired-at tower))))

(defn angle-tower
  "Sets the angle of a tower to face a target."
  {:test (fn []
           (is (= (as-> (create-game {:towers {"t1" (create-tower "Basic" [0 0] :id "t1")}}) $
                        (angle-tower $ (get-tower $ "t1") {:y 48.0 :x 16.0})
                        (get-angle $ "t1"))
                  (/ Math/PI 2))))}
  [state tower target]
  (update-tower state (:id tower)
                (fn [old] (assoc old :angle (calculate-angle (:y tower) (:x tower)
                                                             (:y target) (:x target))))))

(defn shoot
  [state tower target]
  (-> (damage-monster state (:id target) (get-damage state tower))
      (update-tower (:id tower) (fn [old] (assoc old :fired-at (:current-tick state))))))

(defn attempt-to-shoot
  [state monsters tower]
  (let [target (find-target monsters (:y tower) (:x tower) (get-range state tower))]
    (if (not (nil? target))
      (as-> (angle-tower state tower target) $
            (if (ready-to-shoot? $ tower)
              (shoot $ tower target)
              $))
      state)))

(defn all-towers-attempt-to-shoot
  [state]
  (let [monsters (get-monsters state)
        towers (get-towers state)]
    (reduce (fn [new-state tower] (attempt-to-shoot new-state monsters tower)) state towers)))

(defn remove-dead-monsters
  {:test (fn [] (is (= (-> (create-game {:monsters {"m1" (create-monster "Blob" :id "m1" :damage-taken 300)
                                                    "m2" (create-monster "Blob" :id "m2")}})
                           (remove-dead-monsters)
                           (:monsters)
                           (keys))
                       ["m2"])))}
  [state]
  (reduce (fn [new_state monster]
            (if (is-dead? monster)
              (remove-monster new_state (:id monster))
              new_state))
          state
          (get-monsters state)))