(ns tower-defence.game.definitions.towers)

(def basic-tower-definitions
  {"Pea Shooter"   {:name        "Pea Shooter"
                    :cost        10
                    :range       64
                    :rate        1500
                    :damage      10
                    :projectile  "Pea Slow"
                    :description "It shoots peas..."}
   "MGT-MK1"       {:name        "MGT-MK1"
                    :cost        50
                    :range       100
                    :rate        100
                    :damage      3
                    :projectile  "Pea Fast"
                    :description "Cheapest machine gun<br>tower on the market."}
   "Cannon"        {:name        "Cannon"
                    :cost        100
                    :range       100
                    :rate        3000
                    :damage      30
                    :projectile  "Cannonball"
                    :description "Shoots unstoppable<br>cannonballs."}
   "Snow Cannon"   {:name        "Snow Cannon"
                    :cost        200
                    :range       70
                    :rate        2000
                    :damage      10
                    :projectile  "Snowball"
                    :description "Slows enemies that are<br>near the area of impact."
                    :upgrade     "Snow Cannon II"}
   "Venom Spitter" {:name        "Venom Spitter"
                    :cost        200
                    :range       150
                    :rate        1000
                    :damage      10
                    :projectile  "Venom"
                    :description "Spits venom that slows<br>enemies."
                    :upgrade     "Venom Spitter II"}})

(def upgraded-tower-definitions
  {"Snow Cannon II"   {:name          "Snow Cannon II"
                       :cost          500
                       :range         90
                       :rate          2000
                       :damage        40
                       :projectile    "Snowball II"
                       :description   "Slows enemies that are<br>near the area of impact."
                       :upgraded-from "Snow Cannon"}
   "Venom Spitter II" {:name          "Venom Spitter II"
                       :cost          350
                       :range         150
                       :rate          1000
                       :damage        25
                       :projectile    "Venom II"
                       :description   "Spits venom that slows<br>enemies."
                       :upgraded-from "Venom Spitter"}})

(def projectile-definitions
  {"Pea Slow"    {:speed 100
                  :class :single-target}
   "Pea Fast"    {:speed 180
                  :class :single-target}
   "Cannonball"  {:speed 180
                  :class :rolling}
   "Snowball"    {:speed            120
                  :class            :explosive
                  :debuff           "Snowslow"
                  :explosion-radius 40}
   "Snowball II" {:speed            120
                  :class            :explosive
                  :debuff           "Snowslow II"
                  :explosion-radius 40}
   "Venom"       {:speed  120
                  :class  :single-target
                  :debuff "Venom-db"}
   "Venom II"    {:speed  120
                  :class  :single-target
                  :debuff "Venom-db II"}})

(def debuff-definitions
  {"Snowslow"    {:speed    0.7
                  :duration 4000}
   "Snowslow II" {:speed    0.5
                  :duration 5000}
   "Venom-db"    {:speed    0.85
                  :duration 6000}
   "Venom-db II" {:speed    0.7
                  :duration 6000}})