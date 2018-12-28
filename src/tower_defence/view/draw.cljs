(ns tower-defence.view.draw)

(defn save
  [ctx]
  (.save ctx))

(defn restore
  [ctx]
  (.restore ctx))

(defn circle
  [ctx x y radius]
  (.beginPath ctx)
  (.arc ctx x y radius 0 (* 2 Math/PI))
  (.stroke ctx))

(defn fill
  [ctx r g b]
  (save ctx)
  (set! (.-fillStyle ctx) (str "rgb(" r ", " g ", " b ")"))
  (.fill ctx)
  (restore ctx))

(defn set-global-alpha!
  [ctx alpha]
  (set! (.-globalAlpha ctx) alpha))