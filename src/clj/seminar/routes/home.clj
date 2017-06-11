(ns seminar.routes.home
  (:require [seminar.layout :as layout]
            [seminar.controller :as controller]
            [seminar.model :as model]
            [compojure.core :refer [defroutes GET POST ANY]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"
                 {:add-css ["/css/my-style.css"]}))

(def dummy-seminar
  [{:sisa_kuota "48",
     :pembicara "Agus",
     :kuota "50",
     :seminar_id "1",
     :tempat "Auditorium Mercu Buana",
     :jadwal "2017-07-28 08:55:00",
     :status 1,
     :link_peserta "List Peserta",
     :tema "Big Data",
    :no "4"}
   {:sisa_kuota "99",
    :pembicara "Budi",
    :kuota "100",
    :seminar_id "2",
    :tempat "Auditorium Mercu Buana",
    :jadwal "2017-08-17 08:00:00",
    :status 1,
    :link_peserta "List Peserta",
    :tema "IT Security",
    :no "3"}])

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/view-register" ctx (controller/view-register ctx))
  (GET "/list-member" ctx (controller/get-list-member ctx))
  (GET "/list-seminar" ctx (controller/get-list-seminar ctx))
  (GET "/history-order" ctx (controller/view-history-order ctx))
  (GET "/cetak-ticket/:id" [id] (controller/cetak-ticket id))
  (GET "/email-preview" ctx (layout/render "email-notif.html" {:listSeminar dummy-seminar}))

  ;; AJAX PROCESS
  (ANY "/register-member" ctx (controller/register-member ctx))
  (POST "/order-seminar" ctx (controller/order-seminar ctx))
  (POST "/get-history-order" ctx (controller/get-history-order ctx)))

