(ns tower-defence.view.draw
  (:require [cljs.core.async :refer [chan
                                     put!]]))

(defn save
  [ctx]
  (.save ctx))

(defn rotate
  [ctx angle]
  (.rotate ctx angle))

(defn restore
  [ctx]
  (.restore ctx))

(defn translate
  [ctx x y]
  (.translate ctx x y))

(defn draw-image
  ([ctx image x y]
   (.drawImage ctx image x y))
  ([ctx image x y width height]
   (.drawImage ctx image x y width height))
  ([ctx image sx sy swidth sheight dx dy dwidth dheight]
   (.drawImage ctx image sx sy swidth sheight dx dy dwidth dheight)))

(defn draw-text
  [ctx text x y]
  (.fillText ctx text x y))

(defn circle
  [ctx x y radius]
  (.beginPath ctx)
  (.arc ctx x y radius 0 (* 2 Math/PI))
  (.stroke ctx))

(defn set-font!
  [ctx font]
  (set! (.-font ctx) font))

(defn fill
  [ctx r g b]
  (save ctx)
  (set! (.-fillStyle ctx) (str "rgb(" r ", " g ", " b ")"))
  (.fill ctx)
  (restore ctx))

(defn set-global-alpha!
  [ctx alpha]
  (set! (.-globalAlpha ctx) alpha))

(defn draw-chan
  []
  (let [channel (chan)
        request-anim #(.requestAnimationFrame js/window %)]
    (letfn [(trigger-redraw []
              (put! channel 1)
              (request-anim trigger-redraw))]
      (request-anim trigger-redraw)
      channel)))

(defn get-image
  [path]
  (let [img (js/Image.)]
    (aset img "src" path)
    img))