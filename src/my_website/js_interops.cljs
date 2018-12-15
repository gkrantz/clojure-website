(ns my-website.js-interops)

(defn set-url-path
  [path]
  (js/window.history.pushState path path path))

(defn get-url-path
  []
  (.-location.pathname js/window))
