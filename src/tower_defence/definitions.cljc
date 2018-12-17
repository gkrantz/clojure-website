(ns tower-defence.definitions)

(defonce definitions-atom (atom {}))

(defn add-definitions! [definitions]
  (swap! definitions-atom merge definitions))

(defn get-definition
  [name]
  (as-> (-> (deref definitions-atom)
            (get name)) $
        (do (when (nil? $)
              (println "Couldn't load definition" name ", are the definitions loaded?"))
            $)))