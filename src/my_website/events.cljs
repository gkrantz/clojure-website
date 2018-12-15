(ns my-website.events
  (:require [my-website.js-interops :as js-interops]))

(def events
  {:rename-user (fn [state-atom {name :name}]
                  (swap! state-atom (fn [old-state] (assoc-in old-state [:user :name] name))))
   :route       (fn [state-atom {to :to _ :from}]
                  (swap! state-atom (fn [old-state] (assoc old-state :location to)))
                  (js-interops/set-url-path to))})