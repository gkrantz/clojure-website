(ns tower-defence.view.sprites)

(def TICKS_PER_SECOND 24)
(def MS_PER_TICK (/ 1000 TICKS_PER_SECOND))

(def monster-sprite-definitions
  {"Spider" {:cell     0
             :count    3
             :interval 200}
   "Blob"   {:cell     3
             :count    3
             :interval 350}})

(defn get-image
  [path]
  (let [img (js/Image.)]
    (aset img "src" path)
    img))

(def monster-sprite-sheet (get-image "images/tower-defence/monster-sprite-sheet.png"))

(def frame-atom (atom {}))

(defn reset-frame-counters!
  []
  (reset! frame-atom {}))

(defn cell->x
  [cell]
  (* 32 cell))

(defn cell->y
  [cell]
  0)

(defn get-monster-image-args!
  [monster]
  (let [id (:id monster)
        time (or (get @frame-atom id) 0)
        sprite-def (get monster-sprite-definitions (:name monster))]
    (swap! frame-atom (fn [old] (update old id + MS_PER_TICK)))
    (as-> (int (/ time (:interval sprite-def))) $
          (mod $ (:count sprite-def))
          (+ $ (:cell sprite-def))
          [monster-sprite-sheet (cell->x $) (cell->y $) 32 32 -16 -16 32 32])))