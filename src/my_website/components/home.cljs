(ns my-website.components.home
  (:require [rum.core :as rum]
            [cljs.core.async :refer [go-loop <! timeout put! chan]]))

(def links
  [{:img "GitHub-Mark-64px.png"
    :url "https://github.com/gkrantz"}
   {:img "In-2C-41px-R.png"
    :url "https://www.linkedin.com/in/gustav-krantz-6118a89b/"}])

(defn draw-in-circle!
  [img]
  (let [ctx (.getContext (.getElementById js/document "picture-canvas") "2d")]
    (.arc ctx 75 75 75 0 (* 2 Math/PI))
    (.clip ctx)
    (.drawImage ctx img 25 35 200 200 0 0 150 150)))

; We are unsure which fires first of after-render and img.onload.
; Also img.onload does not fire when we change back to this page from another page.
; So we need to draw the image on both events.
(def my-face
  (let [img (js/Image.)]
    (set! (.-onload img) (fn [] (draw-in-circle! img)))
    (aset img "src" "/images/gustav.png")
    img))

(rum/defc component < {:after-render (fn [e]
                                       (draw-in-circle! my-face)
                                       e)}
  []
  [:div
   [:div {:style {:float "left"}}
    [:canvas {:id     "picture-canvas"
              :height 150
              :width  150
              :style  {:user-select      "none"}}]
    [:br]
    (map (fn [{img :img url :url}]
           [:a {:href url
                :key  url}
            [:img {:style {:margin-right "10px"
                           :width        "auto"
                           :height       "40px"}
                   :src   (str "images/" img)}]])
         links)]
   [:div {:style {:display     "inline-block"
                  :margin-left 20}}
    [:h2 {:style {:float "left"}}
     "My website"]
    [:br]
    [:div {:style {:float "left"}}
     "Some text about me or something interesting."]]])
