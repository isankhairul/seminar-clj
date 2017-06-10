(ns seminar.routes.home
  (:require [seminar.layout :as layout]
            [seminar.controller :as controller]
            [seminar.model :as model]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"
                 {:add-css ["/css/my-style.css"]}))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/register-member" [] (layout/render "register-member.html"))
  (GET "/list-member" ctx (controller/get-list-member ctx))
  (GET "/list-seminar" ctx (controller/get-list-seminar ctx))
  (POST "/ajax/order-seminar" ctx (controller/ajax-order-seminar ctx))
  (GET "/history-order" ctx (controller/view-history-order ctx))
  (POST "/get-history-order" ctx (controller/get-history-order ctx))
  (GET "/cetak-ticket/:id" [id] (controller/cetak-ticket id)))

