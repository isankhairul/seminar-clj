(ns seminar.model
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s]
            [clojure.java.jdbc :as jdbc]
            [korma
             [core :refer :all :as kc]
             [db :refer :all :as kdb]]
            [clj-time
             [core   :as tc]
             [format :as tf]
             [coerce :as tcc]
             [local  :as tl]]))

(def db-jdbc {:classname "com.mysql.jdbc.Driver"
              :subprotocol "mysql"
              :subname (str "//127.0.0.1"
                            "3306"
                            "/monica")
              :user "root"
              :password ""})

(defdb db (mysql
           {:db "seminar_scraping"
            :user "root"
            :password ""
            :host "127.0.0.1"
            :port "3306"}))

(defentity seminar
  (pk :seminarId))

(defentity member
  (pk :memberId))

(defentity seminar_order
  (pk :orderId))


