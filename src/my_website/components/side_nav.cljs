(ns my-website.components.side-nav
  (:require [rum.core :as rum]))

(rum/defc component
  "A sidenav component."
  []
  [:div {:key   "sidenav"
         :style {:height           "100%"
                 :position         "fixed"
                 :z-index          1
                 :top              1
                 :right            0
                 :background-color "#111"
                 :overflow-x       "hidden"
                 :transition       "0.5s"
                 :padding-top      "60px"
                 :width            0}}])