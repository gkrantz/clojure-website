(ns my-website.core
  (:require [my-website.components.minimalistic-banner :as banner]
            [my-website.components.login-form :as login-form]
            [my-website.components.links :as links]
            [my-website.events :refer [events]]
            [my-website.js-interops :refer [get-url-path]]
            [rum.core :as rum]))

(enable-console-print!)

(defonce state-atom (atom nil))

(defn handle-event
  "Handles an event triggered by a component."
  [{name :name data :data}]
  (println (str "handling event: " name ", with data: " data))
  (as-> (get events name) $
        (when (not (nil? $))
          ($ state-atom data))))

(rum/defc app [state]
  [:div
   (banner/component state handle-event)
   [:div {:style {:align-items     "center"
                  :justify-content "center"
                  :text-align      "center"}}
    (case (:location state)
      "Home" (login-form/component)
      "Links" (links/component)
      "Games" [:div "games component placeholder"])]])

(defn render! [state]
  (rum/mount (app state)
             (js/document.getElementById "app")))

(when (nil? (deref state-atom))
  (add-watch state-atom
             :state-watch
             (fn [_ _ _ state]
               (render! state)))

  (reset! state-atom {:user     {:name "User"}
                      :location "Home"}))

(defn on-js-reload []
  (render! (deref state-atom)))
