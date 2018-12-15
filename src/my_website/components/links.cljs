(ns my-website.components.links
  (:require [rum.core :as rum]))

(def links
  [{:img  "In-2C-41px-R.png"
    :url  "https://www.linkedin.com/in/gustav-krantz-6118a89b/"
    :text "LinkedIn"}
   {:img  "GitHub-Mark-64px.png"
    :url  "https://github.com/gkrantz"
    :text "GitHub"}])

(rum/defc component
  "A component that contains links."
  []
  [:div {:style {:display "inline-block"}}
   (map (fn [{img :img url :url text :text}]
          [:a {:href  url}
           [:div {:style {:color      "black"
                          :overflow-x "hidden"
                          :margin-top "5px"
                          :font-size  "30px"
                          :display    "flex"
                          :float      "left"}}
            [:img {:style {:margin-right "10px"
                           :width        "40px"
                           :height       "40px"}
                   :src   (str "images/" img)}]
            text]
           [:br]])
        links)])
