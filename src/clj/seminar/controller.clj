(ns seminar.controller
  (:require [clojure.tools.logging :as log]
            [seminar.util :as u]
            [seminar.layout :as layout]
            [seminar.worker_pool :as sw]
            [seminar.model :as model]
            [seminar.scraping :as ss]
            [clojure.java.io :as io]
            [clojure.string :as s]))


(defn save-member-db
  [list-member]
  (when (not-empty list-member)
    (mapv
     (fn [member]
       (let [data-db (array-map
                      :member_id (:member_id member)
                      :email (:email member)
                      :firstname (:firstname member)
                      :lastname (:lastname member)
                      :gender (:gender member)
                      :dob (:dob member)
                      :phone (:phone member)
                      :status (:status member))]
         (if (empty? (model/-check-table
                        "member" {:member_id (:member_id member)}))
           (model/-insert-table "member" data-db))))
     
     list-member)))

(defn get-list-seminar
  [ctx]
  (when-let [state (sw/take-worker "seminar")]
    (try
      (ss/ensure-logged-in-admin state)
      
      (let [result (->> (ss/perform-get-seminar state {})
                        (sort-by :seminar_id))
            
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
                         (sort-by :member_id))
            _ (log/debug "RESULT" (pr-str result))
            _ (when (not-empty result)
                (save-member-db result))
            
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
            
            {:keys [seminar_id email]} params

            member (some-> (ss/perform-get-member state {})
                           (get-peserta-from-email email)
                           first)
            result (ss/perform-order-seminar state params)
            
            [serial] (when (= "success" (:status result))
                       (let [list-peserta (ss/perform-get-peserta
                                           state {:seminar_id seminar_id})
                             peserta (first (get-peserta-from-email list-peserta email))
                             
                             data-db (array-map
                                      :seminar_id seminar_id
                                      :member_id (:member_id member)
                                      :serial (:serial peserta))]
                         (log/debug "DATA-DB" data-db)
                         ;;save to db
                         (model/-insert-table "seminar_order" data-db)
                         
                         [(:serial peserta)]))

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
                              (sort-by :seminar_id))

            list-seminar-with-peserta  (mapv (fn [seminar]
                                               (let [list-peserta (ss/perform-get-peserta
                                                                   state {:seminar_id (:seminar_id seminar)})]
                                                 (assoc seminar :listPeserta list-peserta)))
                                             list-seminar)
            merge-seminar-peserta (for [seminar list-seminar-with-peserta
                                        peserta (:listPeserta seminar)]
                                    (-> (dissoc seminar :listPeserta)
                                        (merge peserta)))

            filter-peserta (->> (get-peserta-from-email merge-seminar-peserta email)
                                (sort-by :seminar_id)
                                (mapv (fn [idx peserta]
                                        (assoc peserta :no (inc idx)))
                                      (range)))]
        ;;(log/debug "list-seminar-with-peserta" (pr-str list-seminar-with-peserta))
        ;;(log/debug "merge-seminar-peserta" (pr-str merge-seminar-peserta))
        
        (selmer.parser/render-file "result-search-history-order.html"
                                   {:listPeserta filter-peserta}))
      
      (catch Throwable e
        (log/debug "Error History Order" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar" state)))))

(defn cetak-ticket
  [order-id]
  (when-let [state (sw/take-worker "seminar")]
    (try
      (ss/ensure-logged-in-admin state)
      (let [filename (ss/perform-cetak-ticket state {:order_id order-id})
            name (.getName (clojure.java.io/file filename))]
        (log/debug "FILENAME" filename)
        {:headers {"Content-Type" "application/pdf"
                   "content-disposition" (str "attachment; filename=" name)}
         :body (clojure.java.io/input-stream
                (clojure.java.io/file filename))})
      
      (catch Throwable e
        (log/debug "Error History Order" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar" state)))))