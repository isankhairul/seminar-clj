(ns seminar.background_services
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async]
            [seminar.config :as config]
            [seminar.util :as u]
            [seminar.worker_pool :as sw]
            [seminar.model :as model]
            [seminar.scraping :as ss]
            [clojure.string :as s]
            [postal.core :as pc]
            [clj-time
             [core   :as tc]
             [format :as tf]
             [coerce :as tcc]
             [local  :as tl]]))


(defn handler-notification-seminar
  [rs-compare]
  (when (not-empty rs-compare)
    (let [smtp-user (get-in config/credentials [:gmail :smtp-user])
          smtp-pass (get-in config/credentials [:gmail :smtp-pass])
          config-smtp {:host "smtp.gmail.com"
                       :user smtp-user
                       :pass smtp-pass
                       :ssl true}
          now (tf/unparse
               (tf/formatter-local "YYYY-MM-dd H:m:s")
               (tl/local-now))
          
          subject (str "Email notification updated seminar " now)
          html-email (selmer.parser/render-file "email-notif.html" {:listSeminar rs-compare})]
      
      (pc/send-message
       config-smtp
       {:from (str "noreply seminar <" smtp-user ">")
        :to "isankhairul@gmail.com"
        :subject subject
        :body [{:type "text/html"
                :content html-email}]}))))


(defn save-data-seminar
  [list-seminar]
  (when (not-empty list-seminar)
    (mapv (fn [seminar]
            (let [data-seminar-db (array-map
                                   :seminar_id (:seminar_id seminar)
                                   :tema (:tema seminar)
                                   :jadwal (:jadwal seminar)
                                   :tempat (:tempat seminar)
                                   :pembicara (:pembicara seminar)
                                   :kuota (:kuota seminar)
                                   :sisa_kuota (:sisa_kuota seminar)
                                   :status (:status seminar))
                  
                  where {:seminar_id (:seminar_id seminar)}
                  now (tf/unparse
                       (tf/formatter-local "YYYY-MM-dd H:m:s")
                       (tl/local-now))
                  
                  data-update (assoc data-seminar-db :modified_date now)]
              
              (if (empty? (model/check-table "seminar" where))
                (model/insert-table "seminar" data-seminar-db)
                (model/update-table "seminar" data-update  where))))
          
     list-seminar)))

(defn handler-scheduler-check-seminar []
  (log/debug "running scheduler-check-seminar...")
  (when-let [state (sw/take-worker "seminar-scheduler")]
    (try
      (ss/ensure-logged-in-admin state)
      
      (let [data-seminar-db  (model/select-table "seminar")
            data-seminar-scraping (->> (ss/perform-get-seminar state {})
                                       (sort-by :seminar_id))
            
            compare-data-seminar (if (empty? data-seminar-db)
                                   ;; condition seminar db empty
                                   (mapv #(assoc % :keterangan "Seminar Baru")
                                         data-seminar-scraping)
                                   (->> (for [scraping data-seminar-scraping
                                              db data-seminar-db]
                                          (if (= (:seminar_id db) (:seminar_id scraping))
                                            ;;condition different sisa_kuota
                                            (when (not= (:sisa_kuota db) (:sisa_kuota scraping))
                                              (let [keterangan (str "Sisa Kuota lama " (:sisa_kuota db))]
                                                (assoc scraping :keterangan keterangan)))

                                            ;;condition new seminar
                                            (when-not (some #(= (:seminar_id %) (:seminar_id scraping))
                                                            data-seminar-db)
                                              (assoc scraping :keterangan "Seminar Baru"))))
                                        (distinct)
                                        (remove nil?)))]
        
        (log/debug "COMPARE-DATA-SEMINAR" (pr-str compare-data-seminar))
        (save-data-seminar compare-data-seminar)

        ;;send mail notification
        (when (not-empty compare-data-seminar)
          (handler-notification-seminar compare-data-seminar))

        compare-data-seminar)
      
      (catch Throwable e
        (log/debug "Error Handler scheduler check seminar" (pr-str (.getMessage e))))
      (finally
        (sw/give-worker "seminar-scheduler" state)))))


(def ^:dynamic *channel-scheduler-check-seminar* (async/chan 500))

(defn scheduler-check-seminar []
  (async/close! *channel-scheduler-check-seminar*)
  (alter-var-root #'*channel-scheduler-check-seminar*
                  (constantly (async/chan 500)))
  
  (let [timeout-value (* 1 60 1000)]
    (async/go (loop []
                (let [v (async/alt! *channel-scheduler-check-seminar*
                                    false :default :keep-going)]
            (if v
              (do (async/<!! (async/timeout timeout-value))
                  (handler-scheduler-check-seminar)
                  (recur))
              (do (log/debug "Exitting scheduler loop..."))))))
    
    (println "initializing scheduler-check-seminar")))