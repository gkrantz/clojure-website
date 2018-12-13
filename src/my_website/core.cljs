(ns my-website.core
  (:require [my-website.components.banner :as banner]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state-atom (atom nil))

(defn render! [state]
  (rum/mount (banner/component state (fn [new-name]
                                       (swap! state-atom (fn [old-state]
                                                           (assoc old-state :name new-name)))))
             (js/document.getElementById "app")))

(when (nil? (deref state-atom))

  (add-watch state-atom
             :game-loop
             (fn [_ _ _ state]
               (render! state)))

  (reset! state-atom {:name "User"}))

(defn on-js-reload []
  (render! (deref state-atom)))
