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
  (pk :seminar_id)
  (table :seminar))

(defentity member
  (pk :member_id)
  (table :member))

(defentity order_seminar
  (pk :order_id)
  (table :order_seminar)
  (belongs-to seminar {:fk :seminar_id})
  (belongs-to member {:fk :member_id}))

(defn select-table
  [tm ]
  (let [tm (->> tm (symbol) (ns-resolve 'seminar.model) deref)]
    (select tm)))

(defn check-table
  [tm p-where]
  (let [tm (->> tm (symbol) (ns-resolve 'seminar.model) deref)]
    (select tm
            (where p-where))))

(defn insert-table
  [tm m]
  (let [tm (->> tm (symbol) (ns-resolve 'seminar.model) deref)]
    (transaction
     (insert tm (values m)))))

(defn update-table
  [tm p-fields p-where]
  (let [tm (->> tm (symbol) (ns-resolve 'seminar.model) deref)]
    (transaction
     (update tm
              (set-fields p-fields)
              (where p-where)))))

(defn get-order-seminar []
  (select order_seminar
          (with seminar)
          (with member)))
