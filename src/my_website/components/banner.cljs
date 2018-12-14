(ns my-website.components.banner
  (:require [rum.core :as rum]))

(rum/defcs hamburger-button < (rum/local true ::closed)
                              "A menu button."
  [local-state {on-open :on-open on-close :on-close}]
  (let [closed (::closed local-state)
        bar-closed-style {:width            "35px"
                          :height           "5px"
                          :background-color "#333"
                          :margin           "6px 0"
                          :transition       "0.4s"}]
    [:div {:style    {:cursor "pointer"}
           :on-click (fn [_] (if (deref closed)
                               (do (reset! closed false)
                                   (when (not (nil? on-open)) (on-open)))
                               (do (reset! closed true)
                                   (when (not (nil? on-close)) (on-close)))))}
     (map (fn [bar]
            [:div {:style (if (deref closed)
                            bar-closed-style
                            (case bar
                              0 (assoc bar-closed-style :transform "rotate(-45deg) translate(-8px, 6px)")
                              1 (assoc bar-closed-style :opacity "0")
                              2 (assoc bar-closed-style :transform "rotate(45deg) translate(-8px, -8px)")))
                   :key   bar}])
          (range 0 3))]))

(rum/defcs component < (rum/local "Enter name here..." ::name)
                       "A banner component."
  [local-state app-state path-to-name-in-state update-name-fn]
  (let [input-atom (::name local-state)]
    [:div {:style {:background-color "black"
                   :float            "left"
                   ;;:border-radius    "5px"
                   :border-bottom    "5px solid #CCC"
                   :height           40
                   :width            "100%"
                   :position         "relative"
                   :display          "inline-block"}}
     [:div {:style {:float          "left"
                    :color          "white"
                    :font-family    "Comic Sans MS, cursive, sans-serif"
                    :font-size      "16px"
                    :letter-spacing "2px"
                    :word-spacing   "2px"
                    :font-style     "italic"
                    :display        "flex"
                    :align-items    "center"
                    :height         "100%"}}
      "Welcome to my website, " (str (get-in app-state path-to-name-in-state)) "!"]
     [:div {:style {:float       "left"
                    :margin-left 15
                    :width       "auto"
                    :align-items "center"
                    :height      "100%"}}
      [:input {:style     {:border        "1px solid #ccc"
                           :border-radius "4px"
                           :box-sizing    "border-box"
                           :height        "70%"}
               :type      "text"
               :value     (deref input-atom)
               :on-change (fn [x] (reset! input-atom (.. x -target -value)))}]
      [:button {:style    {:background-color "#4CAF50"
                           :color            "white"
                           :margin           "3px"
                           :padding          "6px 6px"
                           :border-radius    "4px"
                           :border           "none"
                           :cursor           "pointer"
                           :height           "70%"}
                :on-click (fn [_] (update-name-fn (deref input-atom)))}
       "Send"]]
     [:div {:style {:float        "right"
                    :margin-right "5px"
                    :display      "inline-block"}} (hamburger-button {:on-open  (fn [] (println "opened"))
                                                                      :on-close (fn [] (println "closed"))})]]))