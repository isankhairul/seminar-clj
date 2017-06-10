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

            add-js ["js/global-helper.js"
                    "js/module/order-seminar.js"]
            
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

(defn get-peserta-from-email
  [list-peserta email]
  (->> list-peserta
       (mapv (fn [peserta]
               (when (= email (:email peserta))
                 peserta)))
       (remove nil?)))

(defn ajax-order-seminar
  [ctx]
  (when-let [state (sw/take-worker "seminar")]
    (try
      (ss/ensure-logged-in-admin state)
      
      (let [params (:params ctx)
            
            {:keys [seminarId email]} params
            
            result (ss/perform-order-seminar state params)
            
            serial (when (= "success" (:status result))
                     (let [list-peserta (ss/perform-get-peserta
                                         state {:seminarId seminarId})
                           peserta (first (get-peserta-from-email list-peserta email))]
                       (:serial peserta)))

            result (assoc result :serial serial)]
        
        (cheshire.core/generate-string result))
      
      (catch Throwable e
        (log/debug "Error Order Seminar" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar" state)))))


(defn view-history-order
  [ctx]
  (layout/render "history-order.html"
                 {:title "History Order"
                  :add-js ["js/global-helper.js"
                           "js/module/history-order-seminar.js"]}))

(defn get-history-order
  [ctx]
  (when-let [state (sw/take-worker "seminar")]
    (try
      (ss/ensure-logged-in-admin state)

      (let [email (get-in ctx [:params :email])
            list-seminar (->> (ss/perform-get-seminar state {})
                              (sort-by :seminarId))

            list-seminar-with-peserta  (mapv (fn [seminar]
                                               (let [list-peserta (ss/perform-get-peserta
                                                                   state {:seminarId (:seminarId seminar)})]
                                                 (assoc seminar :listPeserta list-peserta)))
                                             list-seminar)
            merge-seminar-peserta (for [seminar list-seminar-with-peserta
                                        peserta (:listPeserta seminar)]
                                    (-> (dissoc seminar :listPeserta)
                                        (merge peserta)))

            filter-peserta (->> (get-peserta-from-email merge-seminar-peserta email)
                                (sort-by :seminarId)
                                (mapv (fn [idx peserta]
                                        (assoc peserta :no (inc idx)))
                                      (range)))]
        
        (selmer.parser/render-file "result-search-history-order.html"
                                   {:listPeserta filter-peserta}))
      
      (catch Throwable e
        (log/debug "Error History Order" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar" state)))))