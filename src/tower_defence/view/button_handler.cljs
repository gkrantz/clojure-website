(ns tower-defence.view.button-handler)

(def button-atom (atom {}))

(defn add-button!
  "Adds a button to the atom."
  [id button]
  (swap! button-atom (fn [old] (assoc old id button))))

(defn remove-button!
  "Removes the button with the given id."
  [id]
  (swap! button-atom (fn [old] (dissoc old id))))

(defn draw-buttons!
  "Draws all visible buttons."
  [ctx]
  (doseq [button (vals @button-atom)]
      (doseq [image-args (:images button)]
        (apply #(.drawImage ctx %1 %2 %3 %4 %5 %6 %7 %8 %9) image-args))))

(defn- inside-rect?
  [x y rx ry rwidth rheight]
  (and (>= x rx)
       (>= y ry)
       (< x (+ rx rwidth))
       (< y (+ ry rheight))))

(defn mouse-pressed!
  "Checks hits for all buttons."
  [x y]
  (doseq [{bx :x by :y width :width height :height on-click-function :on-click}
          (vals @button-atom)]
    (when (inside-rect? x y bx by width height)
      (on-click-function))))