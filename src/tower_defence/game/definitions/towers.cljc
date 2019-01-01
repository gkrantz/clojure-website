(ns tower-defence.game.definitions.towers)

(def tower-definitions
  {"Pea Shooter"   {:name       "Pea Shooter"
                    :cost       10
                    :range      64
                    :rate       1500
                    :damage     10
                    :projectile "Pea Slow"}
   "MGT-MK1"       {:name       "MGT-MK1"
                    :cost       50
                    :range      100
                    :rate       100
                    :damage     3
                    :projectile "Pea Fast"}
   "Cannon"        {:name       "Cannon"
                    :cost       100
                    :range      100
                    :rate       3000
                    :damage     30
                    :projectile "Cannonball"}
   "Snow Cannon"   {:name       "Snow Cannon"
                    :cost       200
                    :range      70
                    :rate       2000
                    :damage     10
                    :projectile "Snowball"}
   "Venom Spitter" {:name       "Venom Spitter"
                    :cost       200
                    :range      150
                    :rate       1000
                    :damage     10
                    :projectile "Venom"}})

(def projectile-definitions
  {"Pea Slow"   {:speed 100
                 :class :single-target}
   "Pea Fast"   {:speed 180
                 :class :single-target}
   "Cannonball" {:speed 180
                 :class :rolling}
   "Snowball"   {:speed            120
                 :class            :explosive
                 :debuff           "Snowslow"
                 :explosion-radius 40}
   "Venom"      {:speed  120
                 :class  :single-target
                 :debuff "Venom-db"}})

(def debuff-definitions
  {"Snowslow" {:speed    0.5
               :duration 4000}
   "Venom-db" {:speed    0.7
               :duration 6000}})