(ns tower-defence.helpers
  (:require [tower-defence.definitions :refer [get-definition]]
            [tower-defence.constants :refer [SQUARE_SIZE]]))

(defn calculate-angle
  [y1 x1 y2 x2]
  (let [dy (- y2 y1) dx (- x2 x1)]
    (if (= 0 dx)
      (if (> dy 0)
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
  ([y x]
   [(* (+ y 0.5) SQUARE_SIZE)
    (* (+ x 0.5) SQUARE_SIZE)]))

(defn pixel->square
  [y x]
  ([(int (/ y SQUARE_SIZE))
    (int (/ x SQUARE_SIZE))]))

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
  (keys (:towers state)))

(defn force-add-monster
  "Adds a monster to the state."
  [state monster]
  (assoc-in state [:monsters (:id monster)] monster))

(defn force-add-tower
  "Adds a tower to the state without any checking if it's healthy for the state."
  [state tower [y x]]
  (assoc-in state [:towers [y x]] tower))

(defn reduce-gold
  [state amount]
  (update state :gold (fn [old-value] (- old-value amount))))

(def counter-atom (atom 0))

(defn generate-id
  ([]
   (generate-id ""))
  ([prefix]
   (swap! counter-atom inc)
   (str prefix (deref counter-atom))))