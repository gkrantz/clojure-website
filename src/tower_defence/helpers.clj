(ns tower-defence.helpers
  (:require [tower-defence.definitions :refer [get-definition]]
            [tower-defence.constants :refer [SQUARE_SIZE]]))

(defn calculate-middle-of-square
  [y x]
  [(* (+ y 0.5) SQUARE_SIZE) (* (+ x 0.5) SQUARE_SIZE)])

(defn get-height
  [state]
  (:height state))

(defn get-width
  [state]
  (:width state))

(defn get-start
  [state]
  (:start state))

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