(ns my-website.components.login-form
  (:require [rum.core :as rum]))

(rum/defcs component
  "A login component."
  < (rum/local "u" ::username)
    (rum/local "p" ::password)
  [local-state]
  [:div {:style {:background-color "#888"
                 :border           "3px solid #88E"
                 :border-radius    "15px"
                 :height           300
                 :width            250
                 :display          "inline-block"}}
   (str (deref (::username local-state))
        " : "
        (deref (::password local-state)))])
