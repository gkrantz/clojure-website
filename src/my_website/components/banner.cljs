(ns my-website.components.banner
  (:require [rum.core :as rum]))

(rum/defc component [app-state update-name-fn]
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
    "Welcome to my website, " (str (:name app-state)) "!"]
   [:div {:style {:float       "left"
                  :margin-left 15}}
    [:input {:type      "text"
             :value     (deref input-atom)
             :on-change (fn [x] (reset! input-atom (.. x -target -value)))}]
    [:button {:on-click (fn [_] (update-name-fn (deref input-atom)))}
     "Send"]]]))