(ns my-website.events)

(def events
  {:rename-user (fn [state-atom {name :name}]
                  (swap! state-atom (fn [old-state] (assoc-in old-state [:user :name] name))))})