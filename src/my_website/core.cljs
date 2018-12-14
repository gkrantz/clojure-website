(ns my-website.core
  (:require [my-website.components.banner :as banner]
            [my-website.components.login-form :as login-form]
            [my-website.components.side-nav :as side-nav]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state-atom (atom nil))

(rum/defc app [state]
  [:div
   (banner/component state [:name] (fn [new-name]
                                     (swap! state-atom (fn [old-state]
                                                         (assoc old-state :name new-name)))))
   (banner/component state [:name] (fn [new-name]
                                     (swap! state-atom (fn [old-state]
                                                         (assoc old-state :name new-name)))))
   (login-form/component)
   (side-nav/component)])

(defn render! [state]
  (rum/mount (app state)
             (js/document.getElementById "app")))

(when (nil? (deref state-atom))
  (add-watch state-atom
             :state-watch
             (fn [_ _ _ state]
               (render! state)))

  (reset! state-atom {:name "User"}))

(defn on-js-reload []
  (render! (deref state-atom)))
