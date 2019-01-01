(ns tower-defence.game.core
  (:require [clojure.test :refer [is]]
            [tower-defence.game.definitions :refer [get-definition]]
            [tower-defence.game.pathfinding :refer [find-path]]
            [tower-defence.game.helpers :refer [add-debuff
                                                add-projectile
                                                add-projectile-hit
                                                calculate-angle
                                                calculate-middle-of-square
                                                collision?
                                                create-monster
                                                create-tower
                                                damage-monster
                                                distance
                                                force-add-monster
                                                force-add-tower
                                                generate-id
                                                get-abs-start
                                                get-angle
                                                get-damage
                                                get-end
                                                get-gold
                                                get-health
                                                get-height
                                                get-monster
                                                get-monsters
                                                get-monster-ids
                                                get-monster-wpt
                                                get-tower
                                                get-towers
                                                get-range
                                                get-rate
                                                get-speed
                                                get-start
                                                get-tower-cost
                                                get-tower-locations
                                                get-width
                                                get-wpt
                                                get-x
                                                get-y
                                                in-bounds?
                                                is-dead?
                                                monster-count
                                                set-phase
                                                reached-target?
                                                reduce-gold
                                                reset-current-tick
                                                update-monster
                                                update-tower]]
            [tower-defence.game.constants :refer [TICKS_PER_SECOND
                                                  MS_PER_TICK]]))

(defn create-empty-state
  []
  {:width        12
   :height       12
   :start        [0 11]
   :end          [11 0]
   :gold         100
   :lives        10
   :phase        :build
   :level        :test
   :wave         {:name             "wave 0"
                  :spawned-monsters {}
                  :finished         false}
   :current-tick 0
   :counter      1
   :towers       {}
   :monsters     {}
   :projectiles  {}
   :waypoints    {}})

(defn create-game
  ([]
   (create-game {}))
  ([data]
   (merge (create-empty-state) (or data {}))))

(defn calculate-monster-path
  "Calculates the path the monsters will take in a state."
  {:test (fn []
           (is (= (-> (create-game {:width  3
                                    :height 1
                                    :start  [0 0]
                                    :end    [2 0]})
                      (force-add-tower (create-tower "Pea Shooter" [1 0]))
                      (calculate-monster-path))
                  nil))
           (is (= (-> (create-game {:width  3
                                    :height 1
                                    :start  [0 0]
                                    :end    [2 0]})
                      (calculate-monster-path))
                  [[0 0] [1 0] [2 0]])))}
  [state]
  (as-> (reduce (fn [unwalkables tower-location]
                  (assoc unwalkables tower-location :unwalkable))
                {}
                (get-tower-locations state)) $
        (find-path (get-width state)
                   (get-height state)
                   $
                   (get-start state)
                   (get-end state))))

(defn can-build-tower?
  {:test (fn []
           (is (not (-> (create-game {:width  3
                                      :height 1
                                      :start  [0 0]
                                      :end    [2 0]})
                        (can-build-tower? "Pea Shooter" [1 0]))))
           (is (-> (create-game {:width  3
                                 :height 2
                                 :start  [0 0]
                                 :end    [2 0]})
                   (can-build-tower? "Pea Shooter" [1 0]))))}
  [state name [x y]]
  (and (not (= [x y] (get-start state)))
       (not (= [x y] (get-end state)))
       (not (nil? (calculate-monster-path (force-add-tower state (create-tower "Pea Shooter" [x y])))))
       (>= (get-gold state) (get-tower-cost name))))

(defn build-tower
  "Builds a tower without checks."
  {:test (fn [] (as-> (create-empty-state) $
                      (build-tower $ "Pea Shooter" [1 1])
                      (do (is (= (get-gold $) 90)))))}
  [state name [x y]]
  (as-> (reduce-gold state (get-tower-cost name)) $
        (let [[new_state id] (generate-id $ "t")]
          (force-add-tower new_state (create-tower name [x y] :id id)))))

(defn- create-waypoint
  [fromx fromy [tox toy]]
  {:x     (calculate-middle-of-square tox)
   :y     (calculate-middle-of-square toy)
   :angle (calculate-angle fromx fromy tox toy)})

(defn add-waypoints-to-state
  {:test (fn []
           (is (= (-> (create-empty-state)
                      (add-waypoints-to-state)
                      (:waypoints))
                  {0 {:angle (calculate-angle 0 1 1 0)
                      :x     (calculate-middle-of-square 11)
                      :y     (calculate-middle-of-square 0)}}))
           (is (= (-> (create-game {:start  [0 0]
                                    :end    [1 1]
                                    :towers {"b1" (create-tower "Pea Shooter" [1 0])}
                                    :width  2
                                    :height 2})
                      (add-waypoints-to-state)
                      (:waypoints))
                  {0 {:angle (calculate-angle 0 0 0 1)
                      :x     (calculate-middle-of-square 0)
                      :y     (calculate-middle-of-square 1)}
                   1 {:angle (calculate-angle 0 1 1 1)
                      :x     (calculate-middle-of-square 1)
                      :y     (calculate-middle-of-square 1)}})))}
  [state]
  (as-> (calculate-monster-path state) $
        (reduce (fn [[waypoints [fromx fromy] idx] node]
                  (let [wpt (create-waypoint fromx fromy node)]
                    (if (= (:angle (get waypoints (- idx 1)))
                           (:angle wpt))
                      [(assoc waypoints (- idx 1) wpt) node idx]
                      [(assoc waypoints idx wpt) node (+ idx 1)])))
                [{} (first $) 0]
                (drop 1 $))
        (assoc state :waypoints (first $))))

(defn add-monster-to-board
  ([state name]
   (let [[absx absy] (get-abs-start state)]
     (add-monster-to-board state name absx absy)))
  ([state name absx absy]
   (let [[new_state id] (generate-id state "m")]
     (as-> (create-monster name
                           :x absx
                           :y absy
                           :id id
                           :target-wpt-idx 0) $
           (force-add-monster new_state $)))))

(defn remove-monster
  {:test (fn [] (is (= (-> (create-game {:monsters {"m1" (create-monster "Blob" :id "m1")
                                                    "m2" (create-monster "Blob" :id "m2")}})
                           (remove-monster "m1")
                           (:monsters)
                           (keys))
                       ["m2"])))}
  [state id]
  (update state :monsters (fn [old] (dissoc old id))))

(defn monster-reached-end
  [state id]
  ;;TODO Remove lives etc.
  (remove-monster state id))

(defn next-waypoint
  "Increments the target waypoint for a monster."
  [state id wpt]
  (if (= (:target-wpt-idx (get-monster state id)) (- (count (:waypoints state)) 1))
    (monster-reached-end state id)
    (update-monster state id (fn [m]
                               (-> (assoc m :x (:x wpt))
                                   (assoc :y (:y wpt))
                                   (update :target-wpt-idx inc))))))

(defn check-waypoint
  [state id]
  (let [monster (get-monster state id)
        wpt (get-monster-wpt state id)
        dx (Math/abs (- (:x monster) (:x wpt)))
        dy (Math/abs (- (:y monster) (:y wpt)))]
    (if (and (< dx 2)
             (< dy 2))
      (next-waypoint state id wpt)
      state)))

(defn move-monster
  {:test (fn []
           (is (> (-> (create-game {:start     [0 0]
                                    :end       [1 1]
                                    :width     2
                                    :height    2
                                    :waypoints {0 {:angle (calculate-angle 0 0 0 1)
                                                   :x     (calculate-middle-of-square 0)
                                                   :y     (calculate-middle-of-square 1)}}})
                      (add-monster-to-board "Blob")
                      (move-monster "m1")
                      (get-y "m1"))
                  16.0)))}
  [state id]
  (let [speed (/ (get-speed state id) TICKS_PER_SECOND)
        angle (get-angle state id)
        dx (* speed (Math/cos angle))
        dy (* speed (Math/sin angle))]
    (-> (update-monster state id (fn [old] (update old :x + dx)))
        (update-monster id (fn [old] (update old :y + dy)))
        (update-monster id (fn [old] (assoc old :angle angle)))
        (check-waypoint id))))

(defn move-all-monsters
  [state]
  (reduce (fn [state_ id]
            (move-monster state_ id))
          state
          (get-monster-ids state)))

(defn find-target
  {:test (fn []
           (is (= (-> (find-target [(create-monster "Blob" :id "b1" :x 100 :y 100)
                                    (create-monster "Blob" :id "b2" :x 10 :y 10)] 15 15 10)
                      (:id))
                  "b2"))
           (is (= (find-target [(create-monster "Blob" :id "b1" :y 100 :x 100)] 15 15 10)
                  nil)))}
  [monsters x y range]
  (when (not (empty? monsters))
    (let [monster (first monsters)]
      (if (<= (distance x y (:x monster) (:y monster)) range)
        monster
        (find-target (drop 1 monsters) x y range)))))

(defn ready-to-shoot?
  {:test (fn []
           (is (not (-> (create-game {:current-tick 9})
                        (ready-to-shoot? (create-tower "Pea Shooter" [0 0])))))
           (is (-> (create-game {:current-tick (* TICKS_PER_SECOND 2)})
                   (ready-to-shoot? (create-tower "Pea Shooter" [0 0])))))}
  [state tower]
  (<= (get-rate state tower) (* MS_PER_TICK (- (:current-tick state) (:fired-at tower)))))

(defn angle-tower
  "Sets the angle of a tower to face a target."
  {:test (fn []
           (is (= (as-> (create-game {:towers {"t1" (create-tower "Pea Shooter" [0 0] :id "t1")}}) $
                        (angle-tower $ (get-tower $ "t1") {:x 16.0 :y 48.0})
                        (get-angle $ "t1"))
                  (/ Math/PI 2))))}
  [state tower target]
  (update-tower state (:id tower)
                (fn [old] (assoc old :angle (calculate-angle (:x tower) (:y tower)
                                                             (:x target) (:y target))))))

(defn shoot
  [state tower target]
  (let [name (:projectile (get-definition (:name tower)))
        definition (get-definition name)]
    (as-> {:name   name
           :x      (:x tower)
           :y      (:y tower)
           :damage (get-damage state tower)
           :target (case (:class definition)
                     :explosive (select-keys target [:x :y])
                     :rolling (calculate-angle (:x tower) (:y tower) (:x target) (:y target))
                     :single-target (:id target))} $
          (add-projectile state (:class definition) $)
          (update-tower $ (:id tower) (fn [old] (assoc old :fired-at (:current-tick state)))))))

(defn attempt-to-shoot
  [state monsters tower]
  (let [target (find-target monsters (:x tower) (:y tower) (get-range state tower))]
    (if (not (nil? target))
      (as-> (angle-tower state tower target) $
            (if (ready-to-shoot? $ tower)
              (shoot $ tower target)
              $))
      state)))

(defn all-towers-attempt-to-shoot
  [state]
  (let [low-hp (sort-by get-health (get-monsters state))
        high-hp (reverse low-hp)
        last (sort-by (fn [monster]
                        [(:target-wpt-idx monster)
                         (->> (get-wpt state (:target-wpt-idx monster))
                              (distance monster)
                              (- 0))])
                      high-hp)
        first (reverse last)]
    (reduce (fn [new-state tower]
              (as-> (case (:priority tower)
                      :low-hp low-hp
                      :high-hp high-hp
                      :last last
                      :first first) $
                    (attempt-to-shoot new-state $ tower)))
            state
            (get-towers state))))

(defn remove-dead-monsters
  {:test (fn [] (is (= (-> (create-game {:monsters {"m1" (create-monster "Blob" :id "m1" :damage-taken 300)
                                                    "m2" (create-monster "Blob" :id "m2")}})
                           (remove-dead-monsters)
                           (:monsters)
                           (keys))
                       ["m2"])))}
  [state]
  (reduce (fn [new_state monster]
            (if (is-dead? monster)
              (remove-monster new_state (:id monster))
              new_state))
          state
          (get-monsters state)))

(defn win-game
  [state]
  (println "you won"))

(defn next-wave
  "Loads the next wave name into the state. Resets info."
  [state]
  (let [old-definition (get-definition (get-in state [:wave :name]))
        new-definition (get-definition (:next old-definition))]
    (if (nil? new-definition)
      (win-game state)
      (assoc state :wave {:name             (:next old-definition)
                          :spawned-monsters {}
                          :finished         false}))))

(defn check-if-phase-over
  "Checks if the monster phase is over and then changes phase."
  [state]
  (if (and (get-in state [:wave :finished])
           (= (monster-count state) 0))
    (-> (set-phase state :build)
        (next-wave))
    state))

(defn attempt-to-spawn-monsters
  "Checks if it is time to spawn monsters. And then spawns them."
  [state]
  (let [wave-info (:wave state)
        wave-definition (get-definition (:name wave-info))
        current-time (* (:current-tick state) MS_PER_TICK)]
    (reduce (fn [new-state [class-name class]]
              (let [count_ (or (get-in wave-info [:spawned-monsters class-name]) 0)]
                (if (> (:count class) count_)
                  (as-> (assoc-in new-state [:wave :finished] false) $
                        (if (> (- current-time (* (:interval class) count_)) (:interval class))
                          (-> (add-monster-to-board $ (:name class))
                              (assoc-in [:wave :spawned-monsters class-name] (+ 1 count_)))
                          $))
                  new-state)))
            (assoc-in state [:wave :finished] true)
            (:classes wave-definition))))

(defn reset-tower-fire-timers
  "Resets :fired-at for all towers"
  [state]
  (reduce (fn [new-state tower]
            (update-tower new-state (:id tower) (fn [old]
                                                  (assoc old :fired-at -10000))))
          state
          (get-towers state)))

(defn start-monster-phase
  "Changes from building phase to monster phase."
  [state]
  (-> (reset-tower-fire-timers state)
      (reset-current-tick)
      (add-waypoints-to-state)
      (set-phase :monster)))

(defn projectile-hit
  "Any projectile type hits a monster."
  [state projectile monster projectile-definition]
  (as-> (damage-monster state (:id monster) (:damage projectile)) $
        (if-not (nil? (:debuff projectile-definition))
          (add-debuff $ (:id monster) (:debuff projectile-definition))
          $)))

(defn update-all-single-target-projectiles
  "Update location of a single target projectile, check for collision."
  [state]
  (reduce (fn [new-state projectile]
            (let [definition (get-definition projectile)
                  speed (/ (:speed definition) TICKS_PER_SECOND)
                  target (get-monster state (:target projectile))
                  angle (calculate-angle (:x projectile) (:y projectile) (:x target) (:y target))
                  dx (* speed (Math/cos angle))
                  dy (* speed (Math/sin angle))]
              (if (nil? target)
                new-state
                (if (reached-target? projectile target)
                  (projectile-hit new-state projectile target definition)
                  (as-> (update projectile :x + dx) $
                        (update $ :y + dy)
                        (update-in new-state [:projectiles :single-target] (fn [old] (conj old $))))))))
          (assoc-in state [:projectiles :single-target] [])
          (get-in state [:projectiles :single-target])))

(defn explosive-projectile-hit
  [state projectile]
  (let [definition (get-definition projectile)]
    (reduce (fn [new-state monster]
              (if (<= (distance projectile monster) (:explosion-radius definition))
                (projectile-hit new-state projectile monster definition)
                new-state))
            (add-projectile-hit state projectile)
            (get-monsters state))))

(defn update-all-explosive-projectiles
  [state]
  (reduce (fn [new-state projectile]
            (let [definition (get-definition projectile)
                  speed (/ (:speed definition) TICKS_PER_SECOND)
                  target (:target projectile)
                  angle (calculate-angle (:x projectile) (:y projectile) (:x target) (:y target))
                  dx (* speed (Math/cos angle))
                  dy (* speed (Math/sin angle))]
              (if (reached-target? projectile target)
                (explosive-projectile-hit new-state projectile)
                (as-> (update projectile :x + dx) $
                      (update $ :y + dy)
                      (update-in new-state [:projectiles :explosive] (fn [old] (conj old $)))))))
          (assoc-in state [:projectiles :explosive] [])
          (get-in state [:projectiles :explosive])))

(defn attempt-rolling-projectile-hits
  {:test (fn [] (is (= (-> (create-game {:monsters {"m1" (create-monster "Blob" :id "m1" :x 0 :y 0)}})
                           (attempt-rolling-projectile-hits {:x 0 :y 0 :damage 30})
                           (first)
                           (get-monster "m1")
                           (:damage-taken))
                       30)))}
  [state projectile]
  (reduce (fn [[new-state hit-set] monster]
            (if (and (not (contains? hit-set (:id monster)))
                     (collision? projectile 3 monster 16))
              [(projectile-hit new-state projectile monster (get-definition projectile))
               (conj hit-set (:id monster))]
              [new-state hit-set]))
          [state (or (:hits projectile) #{})]
          (get-monsters state)))

(defn update-all-rolling-projectiles
  [state]
  (reduce (fn [new-state projectile]
            (let [definition (get-definition (:name projectile))
                  speed (/ (:speed definition) TICKS_PER_SECOND)
                  angle (:target projectile)
                  dx (* speed (Math/cos angle))
                  dy (* speed (Math/sin angle))]
              (if-not (in-bounds? state (:x projectile) (:y projectile))
                new-state
                (let [[newer-state hit-list] (attempt-rolling-projectile-hits new-state projectile)]
                  (as-> (update projectile :x + dx) $
                        (update $ :y + dy)
                        (assoc $ :hits hit-list)
                        (update-in newer-state [:projectiles :rolling] (fn [old] (conj old $))))))))
          (assoc-in state [:projectiles :rolling] [])
          (get-in state [:projectiles :rolling])))

(defn update-all-projectiles
  [state]
  (-> (update-all-single-target-projectiles state)
      (update-all-rolling-projectiles)
      (update-all-explosive-projectiles)))

(defn- update-monster-debuffs
  [state monster]
  (as-> (reduce (fn [debuff-timers name-and-time]
                  (let [definition (get-definition (first name-and-time))]
                    (if (>= (second name-and-time) (:duration definition))
                      debuff-timers
                      (assoc debuff-timers (first name-and-time) (+ MS_PER_TICK (second name-and-time))))))
                {}
                (:debuff-timers monster)) $
        (update-monster state (:id monster) (fn [old] (assoc old :debuff-timers $)))))

(defn update-all-monsters-debuffs
  [state]
  (reduce (fn [new-state monster]
            (if (> (count (:debuff-timers monster)) 0)
              (update-monster-debuffs new-state monster)
              new-state))
          state
          (get-monsters state)))