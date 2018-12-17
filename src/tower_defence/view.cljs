(ns tower-defence.view
  (:require [rum.core :as rum]
            [tower-defence.player-api :as game]
            [tower-defence.constants :as constants]))

(defn- get-cells
  [height width]
  (reduce (fn [a y]
            (concat a (reduce (fn [a x]
                                (conj a [y x]))
                              []
                              (range 0 width))))
          []
          (range 0 height)))

(defn- get-towers
  [state]
  (vals (:towers state)))

(defn draw-background
  [state size]
  (map-indexed (fn [id [y x]]
                 [:img {:key   id
                        :src   (str "images/tower-defence/32x32.png")
                        :style {:display          "inline-block"
                                :z-index          0
                                :position         "absolute"
                                :transform        (str "translateY(" (* y size) "px)"
                                                       "translateX(" (* x size) "px)")
                                :height           (str size "px")
                                :width            (str size "px")
                                :background-color "black"}}])
               (get-cells (:height state) (:width state))))

(defn draw-towers
  [state size]
  (map (fn [tower]
         [:img {:key   (:id tower)
                :src   (str "images/tower-defence/basic.png")
                :style {:display          "inline-block"
                        :z-index          1
                        :position         "absolute"
                        :transform        (str "translateY(" (* (first (:square tower)) size) "px)"
                                               "translateX(" (* (second (:square tower)) size) "px)")
                        :height           (str size "px")
                        :width            (str size "px")
                        :background-color "black"}}])
       (get-towers state)))

(defn draw-monsters
  [state]
  [])

(rum/defc game-component
  [state trigger-event]
  (let [size constants/SQUARE_SIZE]
    [:div
     {:key   "game-div"
      :style {:position   "relative"
              :text-align "left"
              :height     (str (* (:height state) size) "px")
              :width      (str (* (:width state) size) "px")}}
     (draw-background state size)
     (draw-towers state size)]))

(rum/defc no-game-component
  [state-atom]
  [:div {:on-click (fn []
                     (reset! state-atom (game/start-game)))}
   "Press here to start game!"])

(rum/defc component
  [state-atom trigger-event]
  (println @state-atom)
  [:div {:style {:width            "400px"
                 :height           "400px"
                 :background-color "green"}}
   (if (nil? @state-atom) (no-game-component state-atom)
                          (game-component @state-atom trigger-event))])