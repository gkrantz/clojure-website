(ns tower-defence.view.main
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [rum.core :as rum]
            [tower-defence.game.player-api :as game]
            [tower-defence.game.constants :as constants]
            [tower-defence.view.button-handler :as buttons]
            [tower-defence.game.helpers :refer [pixel->square
                                                get-tower
                                                get-tower-at
                                                get-towers
                                                get-monsters
                                                get-single-target-projectiles
                                                get-damage
                                                get-range
                                                get-rate]]
            [tower-defence.game.core :refer [can-build-tower?]]
            [tower-defence.view.sprites :refer [reset-frame-counters!
                                                get-monster-image-args!
                                                get-tower-image-args!]]
            [tower-defence.game.definitions.towers :refer [tower-definitions]]
            [tower-defence.view.draw :refer [circle
                                             draw-chan
                                             draw-image
                                             draw-text
                                             fill
                                             get-image
                                             restore
                                             rotate
                                             save
                                             set-font!
                                             set-global-alpha!
                                             translate]]
            [cljs.core.async :refer [<! timeout]]
            [goog.string :refer [format]]))

(def game-atom (atom (game/start-game)))
(def canvas-atom (atom nil))
(def mouse-atom (atom {:x 0 :y 0}))
(def selection-atom (atom nil))

(defn- get-cells
  [width height]
  (reduce (fn [a y]
            (concat a (reduce (fn [a x]
                                (conj a [y x]))
                              []
                              (range 0 width))))
          []
          (range 0 height)))

(def image32x32 (get-image "images/tower-defence/32x32.png"))
(def start-wave (get-image "images/tower-defence/start-wave.png"))
(def menu-background (get-image "images/tower-defence/menu-background.png"))
(def projectile (get-image "images/tower-defence/projectile.png"))

(defn get-state!
  []
  (deref game-atom))

(defn selection!
  []
  (deref selection-atom))

(defn draw-background
  [state ctx]
  (draw-image ctx image32x32 0 0 384 384))

(defn draw-tower
  [ctx tower x y]
  (let [[fixed-args moving-args] (get-tower-image-args! tower)]
    (save ctx)
    (translate ctx x y)
    (apply draw-image ctx fixed-args)
    (rotate ctx (:angle tower))
    (apply draw-image ctx moving-args)
    (restore ctx)))

(defn draw-towers
  [state ctx]
  (doseq [tower (get-towers state)]
    (draw-tower ctx tower (:x tower) (:y tower))))

(defn draw-tower-range
  [state ctx tower]
  (circle ctx (:x tower) (:y tower) (get-range state tower))
  (set-global-alpha! ctx 0.3)
  (fill ctx 0 0 200)
  (set-global-alpha! ctx 1))

(defn draw-tower-selection
  [state ctx tower x y]
  (draw-image ctx image32x32 x y)
  (draw-tower ctx tower (+ x 16) (+ y 16))
  (set-font! ctx "15px Arial")
  (draw-text ctx (:name tower) (+ x 40) (+ y 12))
  (draw-text ctx (str "Damage: " (get-damage state tower)) (+ x 40) (+ y 25))
  (draw-text ctx (str "Fire rate: " (.toFixed (/ (get-rate state tower) 1000) 1) "s") (+ x 40) (+ y 38))
  (draw-text ctx (str "Range: " (get-range state tower)) (+ x 40) (+ y 51))
  (draw-tower-range state ctx tower))

(defn draw-selection!
  [ctx x y]
  (let [selection (selection!)
        state @game-atom]
    (case (:type selection)
      nil nil
      :blueprint nil
      :tower (draw-tower-selection state ctx (get-tower state (:data selection)) x y))))

(defn draw-monsters
  [state ctx]
  (doseq [monster (get-monsters state)]
    (save ctx)
    (translate ctx (:x monster) (:y monster))
    (rotate ctx (:angle monster))
    (apply draw-image ctx (get-monster-image-args! monster))
    (restore ctx)))

(defn draw-placement-helper-tower
  [state ctx]
  (let [{px :x py :y} (deref mouse-atom)
        [x y] (pixel->square px py)]
    (when (= (:type (selection!)) :blueprint)
      (set-global-alpha! ctx 0.5)
      (draw-image ctx image32x32 (* 32 x) (* 32 y))
      (set-global-alpha! ctx 1))))

(defn draw-projectiles
  [state ctx]
  (doseq [{x :x y :y} (get-single-target-projectiles state)]
    (draw-image ctx projectile x y)))

(defn draw-game
  [state ctx]
  (draw-background state ctx)
  (draw-towers state ctx)
  (draw-monsters state ctx)
  (draw-projectiles state ctx)
  (draw-placement-helper-tower state ctx)
  (draw-image ctx menu-background 384 0)                    ;temp
  (buttons/draw-buttons! ctx)
  (draw-selection! ctx 388 45))

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

(defn select-something!
  [type data]
  (reset! selection-atom {:type type :data data}))

(defn mouse-pressed-on-pixel
  "Returns if we should continue to mouse-pressed-on-square."
  [x y]
  (buttons/mouse-pressed! x y)
  true)

(defn mouse-pressed-on-square
  [x y]
  (let [selection (selection!)]
    (as-> (get-tower-at @game-atom x y) $
          (if (nil? $)
            (if (= (:type selection) :blueprint)
              (swap! game-atom (fn [old] (game/attempt-build-tower old (:data selection) x y)))
              (select-something! nil nil))
            (select-something! :tower (:id $))))))

(defn mouse-pressed!
  []
  (let [{px :x py :y} (deref mouse-atom)
        [sqx sqy] (pixel->square px py)]
    (when (and (mouse-pressed-on-pixel px py)
               (< sqx (:width @game-atom)))
      (mouse-pressed-on-square sqx sqy))))

(defn start-wave-button-pressed
  []
  (reset-frame-counters!)
  (swap! game-atom (fn [old] (game/start-monster-wave old))))

(defn add-menu-buttons!
  []
  (buttons/add-button! "start-game" {:x        384
                                     :y        359
                                     :width    150
                                     :height   25
                                     :images   [[start-wave 0 0 150 25 384 359 150 25]]
                                     :on-click #(start-wave-button-pressed)})
  (doseq [[index [_ tower]] (map-indexed vector tower-definitions)]
    (let [x (+ 388 (* 40 index))
          y 6]
      (buttons/add-button! (str "build_" (:name tower)) {:x        x
                                                         :y        y
                                                         :width    32
                                                         :height   32
                                                         :on-click (fn [] (select-something! :blueprint (:name tower)))
                                                         :images   (-> (map (fn [args]
                                                                              (-> (take 5 args)
                                                                                  (concat [x y 32 32])))
                                                                            (get-tower-image-args! tower))
                                                                       (conj [image32x32 0 0 32 32 x y 32 32]))}))))

(defn start-game!
  []
  (add-menu-buttons!)
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
             :onMouseDown (fn [_] (mouse-pressed!))}]])