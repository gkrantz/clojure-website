(ns tower-defence.view
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [rum.core :as rum]
            [tower-defence.player-api :as game]
            [tower-defence.constants :as constants]
            [tower-defence.helpers :refer [pixel->square]]
            [tower-defence.core :refer [can-build-tower?]]
            [cljs.core.async :refer [close! put! chan <! timeout unique alts!]]))

(def game-atom (atom (game/start-game)))
(def canvas-atom (atom nil))
(def mouse-atom (atom {:x 0 :y 0}))

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
  (.drawImage ctx image32x32 0 0 384 384))
  ;(doseq [[y x] (get-cells (:height state) (:width state))]
    ;(.drawImage ctx image32x32 (* x 32) (* y 32))))

(defn draw-towers
  [state ctx]
  (doseq [tower (get-towers state)]
    (.drawImage ctx basic (* (second (:square tower)) 32) (* (first (:square tower)) 32))))

(defn draw-monsters
  [state ctx]
  (doseq [monster (get-monsters state)]
    (.drawImage ctx blob (int (- (:x monster) 9)) (int (- (:y monster) 9)))))

(defn draw-placement-helper-tower
  [state ctx]
  (let [{py :y px :x} (deref mouse-atom)
        [y x] (pixel->square py px)]
    ;(when (can-build-tower? state "Basic" [y x])
      (set! (.-globalAlpha ctx) 0.5)
      (.drawImage ctx basic (* 32 x) (* 32 y))
      (set! (.-globalAlpha ctx) 1)));)

(defn draw-game
  [state ctx]
  (draw-background state ctx)
  (draw-towers state ctx)
  (draw-monsters state ctx)
  (draw-placement-helper-tower state ctx))

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
           (when (= (:phase @game-atom) :build)
             (recur))))                                     ;TODO phase

(defn update-mouse!
  [event]
  (let [canvas @canvas-atom
        rect (.getBoundingClientRect canvas)
        y (- (.-clientY event) (.-top rect))
        x (- (.-clientX event) (.-left rect))]
    (reset! mouse-atom {:x x :y y})))

(defn mouse-pressed!
  []
  (let [{py :y px :x} (deref mouse-atom)
        [y x] (pixel->square py px)]
    (swap! game-atom (fn [old] (game/attempt-build-tower old "Basic" y x)))))

(rum/defc component
  []
  [:div
   [:button {:on-click (fn [] (start-draw-loop!))} "Start Game!"]
   [:button {:on-click (fn [] (reset! game-atom (game/tick @game-atom)))} "Tick!"]
   [:button {:on-click (fn [] (start-tick-loop!))} "Tick timer"]
   [:canvas {:class       "tdgame"
             :id          "canvas0"
             :width       384
             :height      384
             :style       {:background-color "green"}
             :onMouseMove (fn [e] (update-mouse! e))
             :onMouseDown (fn [e] (mouse-pressed!))}]])