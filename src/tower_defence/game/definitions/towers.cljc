(ns tower-defence.game.definitions.towers)

(def tower-definitions
  {"Pea Shooter" {:name       "Pea Shooter"
                  :cost       10
                  :range      64
                  :rate       1500
                  :damage     10
                  :projectile "Pea Slow"}
   "MGT-MK1"     {:name       "MGT-MK1"
                  :cost       50
                  :range      100
                  :rate       100
                  :damage     3
                  :projectile "Pea Fast"}
   "Cannon"      {:name       "Cannon"
                  :cost       100
                  :range      100
                  :rate       3000
                  :damage     30
                  :projectile "Cannonball"}
   "Snow Cannon" {:name       "Snow Cannon"
                  :cost       200
                  :range      70
                  :rate       5000
                  :damage     100000
                  :projectile "Snowball"}})

(def projectile-definitions
  {"Pea Slow"   {:speed 100
                 :class :single-target}
   "Pea Fast"   {:speed 180
                 :class :single-target}
   "Cannonball" {:speed 180
                 :class :rolling}
   "Snowball"   {:speed            120
                 :class            :explosive
                 :explosion-radius 40}})