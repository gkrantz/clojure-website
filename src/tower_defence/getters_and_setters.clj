(ns tower-defence.getters-and-setters
  (:require [tower-defence.definitions :refer [get-definition]]))

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
