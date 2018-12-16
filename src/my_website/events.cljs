(ns my-website.events
  (:require [my-website.js-interops :refer [set-url-path]]))

(def events
  {:route       (fn [state-atom {to :to _ :from}]
                  (swap! state-atom (fn [old-state] (assoc old-state :location to)))
                  (set-url-path to))})