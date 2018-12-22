(ns tower-defence.view
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [rum.core :as rum]
            [tower-defence.player-api :as game]
            [tower-defence.constants :as constants]
            [cljs.core.async :refer [close! put! chan <! timeout unique alts!]]))

(def game-atom (atom (game/start-game)))

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

(defn- get-monsters
  [state]
  (vals (:monsters state)))

(defn get-image
  [path]
  (let [img (js/Image.)]
    (aset img "src" path)
    img))

(def image32x32 (get-image "images/tower-defence/32x32.png"))
(def basic (get-image "images/tower-defence/basic.png"))
(def blob (get-image "images/tower-defence/blob.png"))

(defn draw-chan
  []
  (let [channel (chan)
        request-anim #(.requestAnimationFrame js/window %)]
    (letfn [(trigger-redraw []
              (put! channel 1)
              (request-anim trigger-redraw))]
      (request-anim trigger-redraw)
      channel)))

(defn get-state!
  []
  (deref game-atom))

(defn draw-background
  [state ctx]
  (doseq [[y x] (get-cells (:height state) (:width state))]
    (.drawImage ctx image32x32 (* x 32) (* y 32))))

(defn draw-towers
  [state ctx]
  (doseq [tower (get-towers state)]
    (.drawImage ctx basic (* (second (:square tower)) 32) (* (first (:square tower)) 32))))

(defn draw-monsters
  [state ctx]
  (doseq [monster (get-monsters state)]
    (.drawImage ctx blob (int (- (:x monster) 9)) (int (- (:x monster) 9)))))

(defn draw-game
  [state ctx]
  (draw-background state ctx)
  (draw-towers state ctx)
  (draw-monsters state ctx))

(defn start-draw-loop!
  []
  (let [redraw-chan (draw-chan)
        ctx (as-> (.getElementById js/document "canvas0") $
                  (.getContext $ "2d"))]
    (go-loop [state nil]
             (<! redraw-chan)
             (let [new-state (get-state!)]
               (when (not= state new-state)
                 (draw-game new-state ctx))
               (recur new-state)))))

(rum/defc component
  []
  [:div
   [:button {:on-click (fn [] (start-draw-loop!))} "Start Game!"]
   [:button {:on-click (fn [] (reset! game-atom (game/tick @game-atom)))} "Tick!"]
   [:button {:on-click (fn [] (js/setInterval
                                #(reset! game-atom (game/tick @game-atom))
                                (/ 1000 constants/TICKS_PER_SECOND)))} "Tick timer"]
   [:canvas {:class  "tdgame"
             :id     "canvas0"
             :width  384
             :height 384
             :style  {:background-color "green"}}]])