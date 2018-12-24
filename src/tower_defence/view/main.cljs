(ns tower-defence.view.main
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [rum.core :as rum]
            [tower-defence.player-api :as game]
            [tower-defence.constants :as constants]
            [tower-defence.view.button-handler :as buttons]
            [tower-defence.helpers :refer [pixel->square
                                           get-towers
                                           get-monsters
                                           get-single-target-projectiles]]
            [tower-defence.core :refer [can-build-tower?]]
            [tower-defence.view.sprites :refer [reset-frame-counters!
                                                get-monster-image-args!]]
            [cljs.core.async :refer [close! put! chan <! timeout unique alts!]]))

(def game-atom (atom (game/start-game)))
(def canvas-atom (atom nil))
(def mouse-atom (atom {:x 0 :y 0}))

(defn- get-cells
  [width height]
  (reduce (fn [a y]
            (concat a (reduce (fn [a x]
                                (conj a [y x]))
                              []
                              (range 0 width))))
          []
          (range 0 height)))

(defn get-image
  [path]
  (let [img (js/Image.)]
    (aset img "src" path)
    img))

(def image32x32 (get-image "images/tower-defence/32x32.png"))
(def basic (get-image "images/tower-defence/basic.png"))
(def blob (get-image "images/tower-defence/blob.png"))
(def start-wave (get-image "images/tower-defence/start-wave.png"))
(def menu-background (get-image "images/tower-defence/menu-background.png"))
(def projectile (get-image "images/tower-defence/projectile.png"))

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
  (.drawImage ctx image32x32 0 0 384 384))
;(doseq [[y x] (get-cells (:height state) (:width state))]
;(.drawImage ctx image32x32 (* x 32) (* y 32))))

(defn draw-towers
  [state ctx]
  (doseq [tower (get-towers state)]
    (.drawImage ctx basic (* (first (:square tower)) 32) (* (second (:square tower)) 32))))

(defn draw-monsters
  [state ctx]
  (doseq [monster (get-monsters state)]
    (.save ctx)
    (.translate ctx (:x monster) (:y monster))
    (.rotate ctx (:angle monster))
    (apply #(.drawImage ctx %1 %2 %3 %4 %5 %6 %7 %8 %9) (get-monster-image-args! monster))
    (.restore ctx)))

(defn draw-placement-helper-tower
  [state ctx]
  (let [{px :x py :y} (deref mouse-atom)
        [x y] (pixel->square px py)]
    (set! (.-globalAlpha ctx) 0.5)
    (.drawImage ctx basic (* 32 x) (* 32 y))
    (set! (.-globalAlpha ctx) 1)))

(defn draw-projectiles
  [state ctx]
  (doseq [{x :x y :y} (get-single-target-projectiles state)]
    (.drawImage ctx projectile x y)))

(defn draw-game
  [state ctx]
  (draw-background state ctx)
  (draw-towers state ctx)
  (draw-monsters state ctx)
  (draw-projectiles state ctx)
  (draw-placement-helper-tower state ctx)
  (.drawImage ctx menu-background 384 0)                    ;temp
  (buttons/draw-buttons! ctx))

(defn start-draw-loop!
  []
  (reset! canvas-atom (.getElementById js/document "canvas0"))
  (let [redraw-chan (draw-chan)
        ctx (.getContext @canvas-atom "2d")]
    (go-loop [state nil]
             (<! redraw-chan)
             (let [new-state (get-state!)]
               (when (not (= new-state state))
                 (draw-game new-state ctx))
               (recur new-state)))))

(defn start-tick-loop!
  []
  (go-loop []
           (<! (timeout (/ 1000 constants/TICKS_PER_SECOND)))
           (reset! game-atom (game/tick @game-atom))
           (recur)))

(defn update-mouse!
  [event]
  (let [canvas @canvas-atom
        rect (.getBoundingClientRect canvas)
        x (- (.-clientX event) (.-left rect))
        y (- (.-clientY event) (.-top rect))]
    (reset! mouse-atom {:x x :y y})))

(defn mouse-pressed-on-pixel
  "Returns if we should continue to mouse-pressed-on-square."
  [x y]
  (buttons/mouse-pressed! x y)
  true)

(defn mouse-pressed-on-square
  [x y]
  (swap! game-atom (fn [old] (game/attempt-build-tower old "Basic" x y))))

(defn mouse-pressed!
  []
  (let [{px :x py :y} (deref mouse-atom)
        [sqx sqy] (pixel->square px py)]
    (when (and (mouse-pressed-on-pixel px py)
               (<= sqx (:width @game-atom)))
      (mouse-pressed-on-square sqx sqy))))

(defn start-wave-button-pressed
  []
  (reset-frame-counters!)
  (swap! game-atom (fn [old] (game/start-monster-wave old))))

(defn start-game!
  []
  (buttons/add-button! "start-game" {:x 384 :y 359 :width 150 :height 25 :image start-wave :on-click #(start-wave-button-pressed)})
  (start-draw-loop!)
  (start-tick-loop!))

(rum/defc component
  []
  [:div
   [:button {:on-click (fn [] (start-game!))} "Start Game!"]
   [:canvas {:class       "tdgame"
             :id          "canvas0"
             :width       534
             :height      384
             :style       {:background-color "green"
                           :cursor           "url(images/tower-defence/cursor.png), default"}
             :onMouseMove (fn [e] (update-mouse! e))
             :onMouseDown (fn [e] (mouse-pressed!))}]])