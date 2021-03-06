(ns tower-defence.view.sprites)

(def TICKS_PER_SECOND 24)
(def MS_PER_TICK (/ 1000 TICKS_PER_SECOND))

(def monster-sprite-definitions
  {"Spider"     {:cell     0
                 :count    3
                 :interval 200}
   "Blob"       {:cell     3
                 :count    3
                 :interval 500}
   "Bug"        {:cell     6
                 :count    2
                 :interval 150}
   "War Turtle" {:cell     8
                 :count    4
                 :interval 300}
   "Rat"        {:cell     12
                 :count    4
                 :interval 150}})

(def tower-sprite-definitions
  {"Pea Shooter"      {:fixed  1
                       :moving 2}
   "MGT-MK1"          {:fixed  3
                       :moving 4}
   "Cannon"           {:fixed  0                            ;Maybe leave as nil
                       :moving 5}
   "Snow Cannon"      {:fixed  6
                       :moving 7}
   "Snow Cannon II"   {:fixed  6
                       :moving 7}
   "Venom Spitter"    {:fixed  8
                       :moving 9}
   "Venom Spitter II" {:fixed  8
                       :moving 9}})

(def projectile-sprite-definitions
  {"Pea Fast"    0
   "Pea Slow"    0
   "Cannonball"  1
   "Snowball"    3
   "Snowball II" 3
   "Venom"       2
   "Venom II"    2})

(def animation-definitions
  {"Snowball"    {:start    0
                  :count    4
                  :interval 100}
   "Snowball II" {:start    0
                  :count    4
                  :interval 100}})

(defn get-image
  [path]
  (let [img (js/Image.)]
    (aset img "src" path)
    img))

(def monster-sprite-sheet (get-image "images/tower-defence/monster-sprite-sheet.png"))
(def tower-sprite-sheet (get-image "images/tower-defence/tower-sprite-sheet.png"))
(def projectile-sprite-sheet (get-image "images/tower-defence/projectile-sprite-sheet.png"))
(def animation-sprite-sheet (get-image "images/tower-defence/animation-sprite-sheet.png"))
(def sell-upgrade-sprite-sheet (get-image "images/tower-defence/sell-upgrade-sprite-sheet.png"))

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

(defn get-tower-image-args!
  [tower]
  (let [{fixed-cell :fixed moving-cell :moving} (get tower-sprite-definitions (:name tower))]
    [[tower-sprite-sheet (cell->x fixed-cell) (cell->y fixed-cell) 32 32 -16 -16 32 32]
     [tower-sprite-sheet (cell->x moving-cell) (cell->y moving-cell) 32 32 -16 -16 32 32]]))

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

(defn get-projectile-image-args!
  [projectile]
  (let [cell (get projectile-sprite-definitions (:name projectile))]
    [projectile-sprite-sheet (* 7 cell) 0 7 7 -3 -3 7 7]))

(defn get-animation-image-args!
  [animation-data]
  (let [sprite-def (get animation-definitions (:name animation-data))]
    (as-> (int (/ (:elapsed-time animation-data) (:interval sprite-def))) $
          (when (< $ (:count sprite-def))
            (as-> (+ $ (:start sprite-def)) $
                  [animation-sprite-sheet (cell->x $) (cell->y $) 32 32 -16 -16 32 32])))))

(defn get-sell-button-image-args!
  [full-price?]
  (as-> (if full-price? 0 72) $
        [sell-upgrade-sprite-sheet $ 0 72 30 0 0 72 30]))

(defn get-upgrade-button-image-args!
  []
  [sell-upgrade-sprite-sheet 144 0 72 30 0 0 72 30])