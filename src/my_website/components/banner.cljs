(ns my-website.components.banner
  (:require [rum.core :as rum]))

(rum/defc component
  "A banner component that can print a welcome message and update the users name in our state given
  a state, a path to the users name in said state and a function to update the name in the state."
  [app-state path-to-name-in-state update-name-fn]
  (let [input-atom (atom "Enter name here...")]
    [:div {:style {:background-color "black"
                   :float            "left"
                   ;;:border-radius    "5px"
                   :border-bottom    "5px solid #CCC"
                   :height           40
                   :width            "100%"}}
     [:div {:style {:float          "left"
                    :color          "white"
                    :font-family    "Comic Sans MS, cursive, sans-serif"
                    :font-size      "16px"
                    :letter-spacing "2px"
                    :word-spacing   "2px"
                    :font-style     "italic"}}
      "Welcome to my website, " (str (get-in app-state path-to-name-in-state)) "!"]
     [:div {:style {:float       "left"
                    :margin-left 15}}
      [:input {:type      "text"
               :value     (deref input-atom)
               :on-change (fn [x] (reset! input-atom (.. x -target -value)))}]
      [:button {:on-click (fn [_] (update-name-fn (deref input-atom)))}
       "Send"]]]))