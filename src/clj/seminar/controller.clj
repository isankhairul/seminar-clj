(ns seminar.controller
  (:require [clojure.tools.logging :as log]
            [seminar.util :as u]
            [seminar.layout :as layout]
            [seminar.worker_pool :as sw]
            [seminar.scraping :as ss]
            [clojure.java.io :as io]
            [clojure.string :as s]))


(defn get-list-seminar
  [ctx]
  (when-let [state (sw/take-worker "seminar")]
    (try
      (ss/ensure-logged-in-admin state)
      
      (let [result (->> (ss/perform-get-seminar state {})
                        (sort-by :seminarId))

            add-js ["js/module/order-seminar.js"]
            
            data {:title "Order Seminar"
                  :add-js add-js
                  :listSeminar result}]
        
        (layout/render "order-seminar.html" data))
      
      (catch Throwable e
        (log/debug "Error List Seminar" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar" state)))))


(defn get-list-member
  [ctx]
  (when-let [state (sw/take-worker "seminar")]
    (try
      (ss/ensure-logged-in-admin state)
      
      (let [result  (->> (ss/perform-get-member state {})
                         (sort-by :memberId))

            data {:title "List Member"
                  :listMember result}]
        
        (layout/render "list-member.html" data))
      
      (catch Throwable e
        (log/debug "Error List Member" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar" state)))))

(defn get-serialnumber-from-email
  [rs-peserta email]
  (let []))


(defn ajax-order-seminar
  [ctx]
  (when-let [state (sw/take-worker "seminar")]
    (try
      (ss/ensure-logged-in-admin state)
      
      (let [params (:params ctx)
            
            {:keys [:seminarId email]} params
            
            result (ss/perform-order-seminar state params)
            
            serial (when (= "success" (:status result))
                     (let []))
            ]
        (cheshire.core/generate-string result))
      
      (catch Throwable e
        (log/debug "Error Order Seminar" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar" state)))))