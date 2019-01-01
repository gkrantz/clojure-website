(ns my-website.components.minimalistic-banner
  (:require [rum.core :as rum]))

(rum/defc component
  "A minimalistic banner."
  [app-state trigger-event]
  [:div {:style {:height          80
                 :width           "100%"
                 :align-items     "center"
                 :justify-content "center"
                 :text-align      "center"}}
   (let [current-location (:location app-state)]
     (map
       (fn [loc] [:div {:key      (str loc)
                        :style    {:display     "inline-block"
                                   :margin      30
                                   :font-size   "30px"
                                   :cursor      "pointer"
                                   :user-select "none"
                                   :color       (if (= loc current-location)
                                                  "#EFEFEF"
                                                  "black")}
                        :on-click (fn [] (trigger-event {:name :route
                                                         :data {:to   loc
                                                                :from current-location}}))}
                  (str loc)])
       ["Home" "Game"]))])