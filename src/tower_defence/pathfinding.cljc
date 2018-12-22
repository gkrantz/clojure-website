(ns tower-defence.pathfinding
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

(defn visited?
  [y x parents]
  (not (nil? (get parents [y x]))))

(defn in-bounds?
  [y x height width]
  (and (>= y 0)
       (>= x 0)
       (< y height)
       (< x width)))

(defn walkable?
  [y x height width parents]
  (and
    (in-bounds? y x height width)
    (not (= (get parents [y x]) :unwalkable))))

(defn add-children
  "Adds children of a square to the queue and their parent-child relation to the map."
  [y x height width parents queue]
  (reduce (fn [[parents_ queue_] [dy dx]]
            (let [ny (+ y dy) nx (+ x dx)]
              (if (and
                    (not (visited? ny nx parents_))
                    (walkable? ny nx height width parents_)
                    (or (= 0 dx)
                        (= 0 dy)
                        (and (walkable? ny x height width parents_)
                             (walkable? y nx height width parents_))))
                [(assoc parents_ [ny nx] [y x]) (conj queue_ [ny nx])]
                [parents_ queue_])))
          [parents queue]
          neighbour-deltas))

(defn iterate-queue
  "Pops from queue and adds the children of the popped element if any.
  returns a map of parent-child relations."
  {:test (fn []
           (is (= (->> (conj empty-queue [0 0])
                       (iterate-queue 2 2 {}))
                  {[1 0] [0 0]
                   [1 1] [0 0]
                   [0 1] [0 0]
                   [0 0] [1 1]
                   ; start square gets a useless parent
                   })))}
  [height width parents queue]
  (let [yx (peek queue)]
    (if (nil? yx)
      parents
      (let [[parents_ queue_] (add-children (first yx) (last yx)
                                            height width
                                            parents (pop queue))]
        (iterate-queue height width parents_ queue_)))))

(defn get-path
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
                  [[1 0] [0 1]]))
           (is (nil? (find-path
                       3 1 {[1 0] :unwalkable})))
           (is (= (find-path 2 5 {[1 1] :unwalkable
                                  [0 3] :unwalkable})
                  [[1 0] [0 0] [0 1] [0 2] [1 2] [1 3] [1 4] [0 4]])))}
  ([height width unwalkable]
   (find-path height width unwalkable [(- height 1) 0] [0 (- width 1)]))
  ([height width unwalkable from to]
   (let [parents (->> (conj empty-queue from)
                      (iterate-queue height width unwalkable))]
     (when (not (nil? (get parents to)))                    ; Path found
       (get-path from parents [to])))))