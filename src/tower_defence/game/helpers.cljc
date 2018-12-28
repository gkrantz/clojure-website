(ns tower-defence.game.helpers
  (:require [tower-defence.game.definitions :refer [get-definition]]
            [tower-defence.game.constants :refer [SQUARE_SIZE]]
            [clojure.test :refer [is]]))

;; This namespace is for functions with low level of abstraction.

(defn distance
  {:test (fn []
           (is (= (distance 1 1 4 5)
                  5.0))
           (is (= (distance {:x 1 :y 1} {:x 4 :y 5})
                  5.0)))}
  ([item1 item2]
   (distance (:x item1) (:y item1) (:x item2) (:y item2)))
  ([x1 y1 x2 y2]
   (Math/sqrt (+ (Math/pow (- y2 y1) 2) (Math/pow (- x2 x1) 2)))))

(defn calculate-angle
  [x1 y1 x2 y2]
  (let [dx (- x2 x1) dy (- y2 y1)]
    (if (< (Math/abs dx) 0.0001)
      (if (> y2 y1)
        (/ Math/PI 2)
        (/ Math/PI -2))
      (if (> dx 0)
        (Math/atan (/ dy dx))
        (+ (Math/atan (/ dy dx)) Math/PI)))))

(defn calculate-middle-of-square
  ([pair-or-single]
   (if (coll? pair-or-single)
     (calculate-middle-of-square (first pair-or-single)
                                 (last pair-or-single))
     (* (+ pair-or-single 0.5) SQUARE_SIZE)))
  ([x y]
   [(* (+ x 0.5) SQUARE_SIZE)
    (* (+ y 0.5) SQUARE_SIZE)]))

(defn pixel->square
  [x y]
  [(int (/ x SQUARE_SIZE))
   (int (/ y SQUARE_SIZE))])

(defn get-height
  [state]
  (:height state))

(defn get-width
  [state]
  (:width state))

(defn get-start
  [state]
  (:start state))

(defn get-abs-start
  [state]
  (calculate-middle-of-square (get-start state)))

(defn get-end
  [state]
  (:end state))

(defn get-gold
  [state]
  (:gold state))

(defn get-tower-cost
  [name]
  (-> (get-definition name)
      (get :cost)))

(defn get-tower-locations
  [state]
  (reduce (fn [locations [_ v]]
            (conj locations (:square v))) [] (:towers state)))

(defn force-add-monster
  "Adds a monster to the state."
  [state monster]
  (assoc-in state [:monsters (:id monster)] monster))

(defn force-add-tower
  "Adds a tower to the state without any checking if it's healthy for the state."
  [state tower]
  (assoc-in state [:towers (:id tower)] tower))

(defn reduce-gold
  [state amount]
  (update state :gold (fn [old-value] (- old-value amount))))

(defn generate-id
  ([state]
   (generate-id state ""))
  ([state prefix]
   [(update state :counter inc) (str prefix (:counter state))]))

(defn create-monster
  [name & kvs]
  (let [monster {:name         name
                 :damage-taken 0}]
    (if (empty? kvs)
      monster
      (apply assoc monster kvs))))

(defn create-tower
  "Creates a tower given a name."
  {:test (fn [] (is (= (create-tower "Pea Shooter" [1 0])
                       {:name     "Pea Shooter"
                        :fired-at 0
                        :angle    0
                        :x        48.0
                        :y        16.0
                        :square   [1 0]})))}
  [name [x y] & kvs]
  (let [tower {:name     name
               :fired-at 0
               :angle    0
               :x        (calculate-middle-of-square x)
               :y        (calculate-middle-of-square y)
               :square   [x y]}]
    (if (empty? kvs)
      tower
      (apply assoc tower kvs))))

(defn get-monster-ids
  [state]
  (keys (:monsters state)))

(defn get-monster
  [state id]
  (get-in state [:monsters id]))

(defn get-monsters
  [state]
  (vals (:monsters state)))

(defn get-tower
  [state id]
  (get-in state [:towers id]))

(defn get-towers
  [state]
  (vals (:towers state)))

(defn get-tower-at
  [state x y]
  (as-> (get-towers state) $
        (filter (fn [t] (= [x y] (:square t))) $)
        (first $)))

(defn get-rolling-projectiles
  [state]
  (get-in state [:projectiles :rolling]))

(defn get-single-target-projectiles
  [state]
  (get-in state [:projectiles :single-target]))

(defn get-all-projectiles
  [state]
  (concat (get-single-target-projectiles state)
          (get-rolling-projectiles state)))

(defn get-rate
  [state tower]
  (let [definition (get-definition (:name tower))]
    (:rate definition)))

(defn get-range
  [state tower]
  (let [definition (get-definition (:name tower))]
    (:range definition)))

(defn get-speed
  [state id]
  (let [definition (get-definition (:name (get-monster state id)))]
    (:speed definition)))

(defn get-x
  "Only supported for monsters for now."
  [state id]
  (get-in state [:monsters id :x]))

(defn get-y
  "Only supported for monsters for now."
  [state id]
  (get-in state [:monsters id :y]))

(defn get-monster-wpt
  [state id]
  (as-> (get-monster state id) $
        (get-in state [:waypoints (:target-wpt-idx $)])))

(defn get-angle
  [state id]
  (if (= \t (first id))
    (-> (get-tower state id)
        (:angle))
    (-> (get-monster-wpt state id)
        (:angle))))

(defn update-monster
  [state id func]
  (update-in state [:monsters id] func))

(defn update-tower
  [state id func]
  (update-in state [:towers id] func))

(defn damage-monster
  [state id amount]
  (update-monster state id (fn [old]
                             (update old :damage-taken + amount))))

(defn get-damage
  [state tower]
  (let [definition (get-definition (:name tower))]
    (:damage definition)))

(defn get-max-health
  [monster]
  (:health (get-definition (:name monster))))

(defn get-health
  [monster]
  (- (get-max-health monster) (:damage-taken monster)))

(defn is-dead?
  [monster]
  (> 0 (get-health monster)))

(defn set-phase
  [state new-phase]
  (assoc state :phase new-phase))

(defn monster-count
  [state]
  (count (get-monsters state)))

(defn reset-current-tick
  [state]
  (assoc state :current-tick 0))

(defn is-monster-phase?
  [state]
  (= :monster (:phase state)))

(defn is-build-phase?
  [state]
  (= :build (:phase state)))

(defn reached-target?
  [projectile target]
  (< (distance (:x projectile) (:y projectile) (:x target) (:y target)) 9))

(defn add-projectile
  [state class projectile]
  (update-in state [:projectiles class] (fn [old] (conj old projectile))))

(defn in-bounds?
  [state x y]
  (and (> x 0)
       (> y 0)
       (<= x (* SQUARE_SIZE (:width state)))
       (<= y (* SQUARE_SIZE (:height state)))))

(defn collision?
  [item1 rad1 item2 rad2]
  (< (distance item1 item2)
     (+ rad1 rad2)))