(ns tower-defence.view.main
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [rum.core :as rum]
            [clojure.string :refer [split]]
            [tower-defence.game.player-api :as game]
            [tower-defence.game.constants :as constants]
            [tower-defence.view.button-handler :as buttons]
            [tower-defence.game.helpers :refer [built-this-phase?
                                                calculate-middle-of-square
                                                create-tower
                                                get-all-projectiles
                                                pixel->square
                                                get-tower
                                                get-tower-at
                                                get-towers
                                                get-monsters
                                                get-damage
                                                get-damage-dealt
                                                get-description
                                                get-range
                                                get-rate]]
            [tower-defence.game.core :refer [can-build-tower?]]
            [tower-defence.view.sprites :refer [reset-frame-counters!
                                                get-animation-image-args!
                                                get-monster-image-args!
                                                get-projectile-image-args!
                                                get-tower-image-args!
                                                get-sell-button-image-args!
                                                get-upgrade-button-image-args!]]
            [tower-defence.game.definitions :refer [get-definition]]
            [tower-defence.game.definitions.towers :refer [basic-tower-definitions]]
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

(defn draw-range-indicator
  [ctx x y range]
  (circle ctx x y range)
  (set-global-alpha! ctx 0.3)
  (fill ctx 0 0 200)
  (set-global-alpha! ctx 1))

(defn draw-tower-selection-stats
  [ctx x y tower damage rate range damage-dealt description]
  (draw-image ctx image32x32 x y)
  (draw-tower ctx tower (+ x 16) (+ y 16))
  (set-font! ctx "bold 15px Arial")
  (draw-text ctx (:name tower) (+ x 35) (+ y 12))
  (set-font! ctx "15px Arial")
  (draw-text ctx (str "Damage: " damage) (+ x 35) (+ y 25))
  (draw-text ctx (str "Fire rate: " (.toFixed (/ rate 1000) 1) "s") (+ x 35) (+ y 38))
  (draw-text ctx (str "Range: " range) x (+ y 51))
  (draw-text ctx (str "Total dmg: " damage-dealt) x (+ y 64)) ; Might need to format this
  (set-font! ctx "italic 15px Arial gray")
  (doseq [[idx line] (map-indexed vector (split description "<br>"))] (draw-text ctx line x (+ y 77 (* idx 13)))))

(defn draw-tower-selection
  [ctx x y state tower]
  (draw-tower-selection-stats ctx x y tower
                              (get-damage state tower)
                              (get-rate state tower)
                              (get-range state tower)
                              (get-damage-dealt tower)
                              (get-description tower))
  (draw-range-indicator ctx (:x tower) (:y tower) (get-range state tower)))

(defn draw-blueprint-selection
  [ctx x y name]
  (let [definition (get-definition name)]
    (draw-tower-selection-stats ctx x y
                                (create-tower name [0 0])
                                (:damage definition)
                                (:rate definition)
                                (:range definition)
                                0
                                (:description definition))))

(defn draw-selection!
  [ctx x y]
  (let [selection @selection-atom
        state @game-atom]
    (case (:type selection)
      nil nil
      :blueprint (draw-blueprint-selection ctx x y (:data selection))
      :tower (draw-tower-selection ctx x y state (get-tower state (:data selection))))))

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
  (when (= (:type @selection-atom) :blueprint)
    (let [{px :x py :y} (deref mouse-atom)
          [sx sy] (pixel->square px py)
          [px2 py2] (calculate-middle-of-square sx sy)
          definition (get-definition (:data @selection-atom))]
      (set-global-alpha! ctx 0.5)
      (draw-tower ctx definition px2 py2)
      (draw-range-indicator ctx px2 py2 (:range definition))
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
  (draw-selection! ctx 388 163)
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
  (let [selection @selection-atom]
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

(defn- add-priority-button
  [x y name type index]
  (buttons/add-button! name {:x        (+ x (* 36 index))
                             :y        y
                             :width    36
                             :height   11
                             :on-click (fn [] (change-tower-priority! type))
                             :draw-fn  (get-priority-button-draw-fn! [priority-buttons (* 36 index) 0 36 11 (+ x (* 36 index)) y 36 11] type)}))

(defn add-menu-buttons!
  []
  (buttons/add-button! "start-game" {:x        384
                                     :y        359
                                     :width    150
                                     :height   25
                                     :draw-fn  (fn [ctx] (draw-image ctx start-wave 0 0 150 25 384 359 150 25))
                                     :on-click #(start-wave-button-pressed)})
  (doseq [[index [_ tower]] (map-indexed vector basic-tower-definitions)]
    (let [x (+ 388 (* 36 (mod index 4)))
          y (+ 43 (* 36 (int (/ index 4))))]
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
  (add-priority-button 387 151 "first" :first 0)
  (add-priority-button 387 151 "last" :last 1)
  (add-priority-button 387 151 "low-hp" :low-hp 2)
  (add-priority-button 387 151 "high-hp" :high-hp 3)
  (let [x 388
        y 258
        width 72
        height 30]
    (buttons/add-button! "sell-tower" {:x        x
                                       :y        y
                                       :width    width
                                       :height   height
                                       :on-click (fn [] (let [selection @selection-atom]
                                                          (when (= :tower (:type selection))
                                                            (swap! game-atom (fn [old] (game/sell-a-tower old (get-tower old (:data selection)))))
                                                            (select-something! nil nil))))
                                       :draw-fn  (fn [ctx]
                                                   (when (= :tower (:type @selection-atom))
                                                     (as-> (built-this-phase? @game-atom (:data @selection-atom)) $
                                                           (get-sell-button-image-args! $)
                                                           (take 5 $)
                                                           (concat $ [x y width height])
                                                           (apply draw-image ctx $))))}))

  (let [x 460
        y 258
        width 72
        height 30]
    (buttons/add-button! "upgrade-tower" {:x        x
                                          :y        y
                                          :width    width
                                          :height   height
                                          :on-click (fn [] (let [selection @selection-atom]
                                                             (when (= :tower (:type selection))
                                                               (swap! game-atom (fn [old] (game/upgrade-a-tower old (get-tower old (:data selection))))))))
                                          :draw-fn  (fn [ctx]
                                                      (when (= :tower (:type @selection-atom))
                                                        (let [definition (get-definition (get-tower @game-atom (:data @selection-atom)))]
                                                          (when-not (nil? (:upgrade definition))
                                                            (let [cost (:cost (get-definition (:upgrade definition)))]
                                                              (as-> (get-upgrade-button-image-args!) $
                                                                    (take 5 $)
                                                                    (concat $ [x y width height])
                                                                    (apply draw-image ctx $))
                                                              (set-font! ctx "bold 12px Arial")
                                                              (draw-text ctx (str cost) (+ x 24) (+ y 24)))))))})))

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