(ns tower-defence.view.main
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [rum.core :as rum]
            [tower-defence.game.player-api :as game]
            [tower-defence.game.constants :as constants]
            [tower-defence.view.button-handler :as buttons]
            [tower-defence.game.helpers :refer [get-all-projectiles
                                                pixel->square
                                                get-tower
                                                get-tower-at
                                                get-towers
                                                get-monsters
                                                get-damage
                                                get-range
                                                get-rate]]
            [tower-defence.game.core :refer [can-build-tower?]]
            [tower-defence.view.sprites :refer [reset-frame-counters!
                                                get-animation-image-args!
                                                get-monster-image-args!
                                                get-projectile-image-args!
                                                get-tower-image-args!]]
            [tower-defence.game.definitions.towers :refer [tower-definitions]]
            [tower-defence.view.draw :refer [circle
                                             draw-chan
                                             draw-image
                                             draw-text
                                             fill
                                             fill-rect
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
(def animation-atom (atom []))

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
(def priority-buttons (get-image "images/tower-defence/priority-buttons.png"))

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
  (set-font! ctx "bold 15px Arial")
  (draw-text ctx (:name tower) (+ x 40) (+ y 12))
  (set-font! ctx "15px Arial")
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
  (doseq [projectile (get-all-projectiles state)]
    (save ctx)
    (translate ctx (:x projectile) (:y projectile))
    (rotate ctx (:angle projectile))
    (apply draw-image ctx (get-projectile-image-args! projectile))
    (restore ctx)))

(defn add-all-pending-animations!
  "Adds animations (currently explosive projectiles only)
  to be drawn until completed."
  [state]
  (doseq [projectile (get-in state [:projectiles :hits-this-turn])]
    (as-> (:target projectile) $
          (assoc $ :name (:name projectile))
          (swap! animation-atom conj $))))

(defn draw-animations!
  "Draws and updates all current animations."
  [ctx]
  (as-> (reduce (fn [new-list animation]
                  (let [args (get-animation-image-args! animation)]
                    (if (nil? args)
                      new-list
                      (do (save ctx)
                          (translate ctx (:x animation) (:y animation))
                          (apply draw-image ctx args)
                          (restore ctx)
                          (as-> (:elapsed-time animation) $
                                (+ $ constants/MS_PER_TICK)
                                (assoc animation :elapsed-time $)
                                (conj new-list $))))))
                []
                @animation-atom) $
        (reset! animation-atom $)))

(defn draw-game
  [state ctx]
  (draw-background state ctx)
  (draw-towers state ctx)
  (draw-monsters state ctx)
  (draw-projectiles state ctx)
  (draw-placement-helper-tower state ctx)
  (draw-image ctx menu-background 384 0)                    ;temp
  (buttons/draw-buttons! ctx)
  (draw-selection! ctx 388 81)
  (draw-animations! ctx))

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
           (add-all-pending-animations! @game-atom)
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

(defn- change-tower-priority!
  [priority]
  (let [selection @selection-atom]
    (when (= :tower (:type selection))
      (swap! game-atom
             (fn [old] (game/change-tower-priority old
                                                   (:data selection)
                                                   priority))))))

(defn- get-priority-button-draw-fn!
  [arg-list priority]
  (fn [ctx]
    (let [selection @selection-atom
          tower (get-tower @game-atom (:data selection))]
      (when (= :tower (:type selection))
        (apply draw-image ctx arg-list)
        (when (= priority (:priority tower))
          (apply fill-rect ctx "rgba(90,90,90,0.4)" (take-last 4 arg-list)))))))

(defn add-menu-buttons!
  []
  (buttons/add-button! "start-game" {:x        384
                                     :y        359
                                     :width    150
                                     :height   25
                                     :draw-fn  (fn [ctx] (draw-image ctx start-wave 0 0 150 25 384 359 150 25))
                                     :on-click #(start-wave-button-pressed)})
  (doseq [[index [_ tower]] (map-indexed vector tower-definitions)]
    (let [x (+ 388 (* 36 (mod index 4)))
          y (+ 6 (* 36 (int (/ index 4))))]
      (buttons/add-button! (str "build_" (:name tower)) {:x        x
                                                         :y        y
                                                         :width    32
                                                         :height   32
                                                         :on-click (fn [] (select-something! :blueprint (:name tower)))
                                                         :draw-fn  (fn [ctx] (do (draw-image ctx image32x32 x y)
                                                                                 (doseq [args (get-tower-image-args! tower)]
                                                                                   (as-> (take 5 args) $
                                                                                         (concat $ [x y 32 32])
                                                                                         (apply draw-image ctx $)))))})))
  (buttons/add-button! "first" {:x        387
                                :y        136
                                :width    36
                                :height   11
                                :on-click (fn [] (change-tower-priority! :first))
                                :draw-fn  (get-priority-button-draw-fn! [priority-buttons 0 0 36 11 387 136 36 11] :first)})
  (buttons/add-button! "last" {:x        423
                               :y        136
                               :width    36
                               :height   11
                               :on-click (fn [] (change-tower-priority! :last))
                               :draw-fn  (get-priority-button-draw-fn! [priority-buttons 36 0 36 11 423 136 36 11] :last)})
  (buttons/add-button! "low-hp" {:x        459
                                 :y        136
                                 :width    36
                                 :height   11
                                 :on-click (fn [] (change-tower-priority! :low-hp))
                                 :draw-fn  (get-priority-button-draw-fn! [priority-buttons 72 0 36 11 459 136 36 11] :low-hp)})
  (buttons/add-button! "high-hp" {:x        495
                                  :y        136
                                  :width    36
                                  :height   11
                                  :on-click (fn [] (change-tower-priority! :high-hp))
                                  :draw-fn  (get-priority-button-draw-fn! [priority-buttons 108 0 36 11 495 136 36 11] :high-hp)}))

(defn start-game!
  []
  (add-menu-buttons!)
  (start-draw-loop!)
  (start-tick-loop!))

(rum/defc component
  []
  [:div
   [:button {:on-click (fn [] (start-game!))} "Start Game!"]
   [:button {:on-click (fn [] (println @game-atom))} "Print state"]
   [:canvas {:class       "tdgame"
             :id          "canvas0"
             :width       534
             :height      384
             :style       {:background-color "green"
                           :cursor           "url(images/tower-defence/cursor.png), default"}
             :onMouseMove (fn [e] (update-mouse! e))
             :onMouseDown (fn [_] (mouse-pressed!))}]])