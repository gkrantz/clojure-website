(ns tower-defence.game.pathfinding
  (:require [clojure.test :refer [is]]))

(def empty-queue
  #?(:clj  clojure.lang.PersistentQueue/EMPTY
     :cljs #queue []))

(def neighbour-deltas
  [[-1 -1]
   [-1 1]
   [1 1]
   [1 -1]
   [-1 0]
   [1 0]
   [0 -1]
   [0 1]])

(defn- visited?
  [x y parents]
  (not (nil? (get parents [x y]))))

(defn- in-bounds?
  [x y height width]
  (and (>= x 0)
       (>= y 0)
       (< x width)
       (< y height)))

(defn- walkable?
  [x y height width parents]
  (and
    (in-bounds? x y width height)
    (not (= (get parents [x y]) :unwalkable))))

(defn- add-children
  "Adds children of a square to the queue and their parent-child relation to the map."
  [x y width height parents queue]
  (reduce (fn [[parents_ queue_] [dx dy]]
            (let [nx (+ x dx)
                  ny (+ y dy)]
              (if (and
                    (not (visited? nx ny parents_))
                    (walkable? nx ny width height parents_)
                    (or (= 0 dx)
                        (= 0 dy)
                        (and (walkable? x ny width height parents_)
                             (walkable? nx y width height parents_))))
                [(assoc parents_ [nx ny] [x y]) (conj queue_ [nx ny])]
                [parents_ queue_])))
          [parents queue]
          neighbour-deltas))

(defn- iterate-queue
  "Pops from queue and adds the children of the popped element if any.
  returns a map of parent-child relations."
  {:test (fn []
           (is (= (->> (conj empty-queue [0 0])
                       (iterate-queue 2 2 {}))
                  {[0 1] [0 0]
                   [1 1] [0 0]
                   [1 0] [0 0]
                   [0 0] [1 1]
                   ; start square gets a useless parent
                   })))}
  [width height parents queue]
  (let [yx (peek queue)]
    (if (nil? yx)
      parents
      (let [[parents_ queue_] (add-children (first yx) (last yx)
                                            width height
                                            parents (pop queue))]
        (iterate-queue width height parents_ queue_)))))

(defn- get-path
  "Gets the final path given child-parent relations.
  res should contains the 'to' coordinates"
  ([from parents res]
   (let [to (first res)]
     (if (= from to)
       res
       (as-> (get parents to) $
             (cons $ res)
             (get-path from parents $))))))

(defn find-path
  "Returns the shortest path from start to end."
  {:test (fn []
           (is (= (find-path
                    2 2 {})
                  [[0 1] [1 0]]))
           (is (nil? (find-path
                       1 3 {[0 1] :unwalkable})))
           (is (= (find-path 5 2 {[1 1] :unwalkable
                                  [3 0] :unwalkable})
                  [[0 1] [0 0] [1 0] [2 0] [2 1] [3 1] [4 1] [4 0]])))}
  ([width height unwalkable]
   (find-path width height unwalkable [0 (- height 1)] [(- width 1) 0]))
  ([width height unwalkable from to]
   (let [parents (->> (conj empty-queue from)
                      (iterate-queue width height unwalkable))]
     (when (not (nil? (get parents to)))                    ; Path found
       (get-path from parents [to])))))