(ns seminar.parse.scraping
  (:require [clojure.tools.logging :as log]
            [clojure
             [zip :as zip]
             [string :as s]]
            [hickory [core :as hc]
             [utils  :as hu]
             [zip    :as hz]
             [select :as hs]
             [render :as hr]]))


(def config
  (let [base-url "http://localhost/seminar"]
    {:urls
     {:login-admin (str base-url "/admin-login")
      :do-login-admin (str base-url "/backend/c_login/do_login")
      :dashboard (str base-url "/dashboard")
      :member (str base-url "/member")
      :seminar-admin (str base-url "/seminar-admin")
      :list-peserta (str base-url "/backend/c_seminar/listPeserta")
      
      ;;front
      :login-front (str base-url "/login")
      :submit-register-member (str base-url "/front/member/submit_register_member")
      :submit-order-seminar (str base-url "/front/seminar/submit_order")
      :cetak-ticket (str base-url "/front/seminar/cetak_ticket")}
     
     :hc-params {:timeout 30000 ;; 30 seconds
                 :user-agent "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 Firefox/39.0"
                 :keep-alive 60000
                 :follow-redirects false
                 :throw-exceptions false
                 :as :text}}))



;; PARSE
(defn parse-loggedadmin
  [html]
  (let [xdoc (-> html hc/parse hc/as-hickory)
        exist? (->> (hs/select
                     (hs/and
                      (hs/tag :ul)
                      (hs/class "user-menu"))
                     xdoc)
                    not-empty boolean)]
    exist?))


(defn parse-pagination
  [html]
  (when html
    (let [xdoc (-> html hc/parse hc/as-hickory)
          pagination (->> (hs/select
                           (hs/and
                            (hs/tag :ul)
                            (hs/class "pagination"))
                           xdoc)
                          not-empty)]
      
      (some->> pagination
               first :content
               (mapv #(get-in % [:content 0 :attrs :href]))
               (drop 1) (drop-last) vec)
      )))

(defn parse-seminar-admin-table-tr
  [tag-tr]
  (some->> tag-tr
           (hs/select
            (hs/tag :td))
           (mapv
            (fn [key tag-td]
              (let [count-content (count (:content tag-td))
                    content (first (:content tag-td))
                    content (cond
                              (= :linkPeserta key) (-> content :attrs :href)
                              (= :status key) (if (= "active" (-> content :content first s/lower-case))
                                                1 0)
                              (> count-content 1) (some->> tag-td
                                                           (hs/select
                                                            (hs/tag :a))
                                                           last
                                                           :attrs :id_delete_seminar)
                              (-> content :content first boolean) (-> content :content first)
                              :else content)]
                [key content]))
            [:no :tema :jadwal :pembicara :tempat :kuota
             :sisa_kuota :link_peserta :status :seminar_id])
           flatten
           (apply hash-map))
  )

(defn parse-seminar-admin
  [html]
  (when html
    (let [xdoc (-> html hc/parse hc/as-hickory)
          table-tr (->> xdoc
                        (hs/select
                         (hs/descendant
                          (hs/class "panel-body")
                          (hs/and
                           (hs/tag :table)
                           (hs/attr :class (fn [s] (re-find (re-pattern "table") s))))
                          (hs/tag :tbody)
                          (hs/tag :tr))))
          
          result (some->> table-tr
                          (mapv parse-seminar-admin-table-tr)
                          (remove empty?))]
      result)))


(defn parse-member-admin-table-tr
  [tag-tr]
  (some->> tag-tr
           (hs/select
            (hs/tag :td))
           (mapv
            (fn [key tag-td]
              (let [count-content (count (:content tag-td))
                    content (first (:content tag-td))
                    content (cond
                              (= :status key) (if (= "active" (-> content :content first s/lower-case))
                                                1 0)
                              
                              (> count-content 1) (some->> tag-td
                                                           (hs/select
                                                            (hs/tag :a))
                                                           last
                                                           :attrs :member_id)
                              (-> content :content first boolean) (-> content :content first)
                              :else content)]
                [key content]))
            [:no :firstname :lastname :email
             :gender :dob :phone :status :member_id])
           flatten
           (apply hash-map))
  )

(defn parse-member-admin
  [html]
  (when html
    (let [xdoc (-> html hc/parse hc/as-hickory)
          table-tr (->> xdoc
                        (hs/select
                         (hs/descendant
                          (hs/class "panel-body")
                          (hs/and
                           (hs/tag :table)
                           (hs/attr :class (fn [s] (re-find (re-pattern "table") s))))
                          (hs/tag :tbody)
                          (hs/tag :tr))))
          
          result (some->> table-tr
                          (mapv parse-member-admin-table-tr)
                          (remove empty?))]
      result)))

(defn parse-peserta-admin-table-tr
  [tag-tr]
  (some->> tag-tr
           (hs/select
            (hs/tag :td))
           (mapv
            (fn [key tag-td]
              (let [count-content (count (:content tag-td))
                    content (first (:content tag-td))
                    _ (log/debug "CONTENT" (pr-str (:content tag-td)))
                    content (cond
                              (= :kehadiran key) (some->> tag-td
                                                          (hs/select
                                                           (hs/tag :input))
                                                          last :attrs :checked
                                                          (nil?) (not))
                              (= :orderId key) (some-> (hs/select
                                                        (hs/tag :a) tag-td)
                                                       last :attrs :href str
                                                       (s/split #"\/")
                                                       last)
                              (-> content :content first boolean) (-> content :content first)
                              :else content)]
                [key content]))
            [:no :kehadiran :fullname :email :serial :tema :order_id])
           flatten
           (apply hash-map))
  )

(defn parse-peserta-admin
  [html]
  (when html
    (let [xdoc (-> html hc/parse hc/as-hickory)
          table-tr (->> xdoc
                        (hs/select
                         (hs/descendant
                          (hs/class "panel-body")
                          (hs/and
                           (hs/tag :table)
                           (hs/attr :class (fn [s] (re-find (re-pattern "table") s))))
                          (hs/tag :tbody)
                          (hs/tag :tr))))
          
          result (some->> table-tr
                          (mapv parse-peserta-admin-table-tr)
                          (remove empty?))]
      result)))



(defn parse-has-error
  [html]
  (when html
    (let [xdoc (-> html hc/parse hc/as-hickory)
          list-error-raw (->> xdoc
                              (hs/select
                               (hs/descendant
                                (hs/class "has-error")
                                (hs/tag :p))))
          list-error (->> list-error-raw
                          (mapv :content)
                          (mapv first))
          ]
      list-error)))

(defn parse-alert-message
  [html]
  (when html
    (let [xdoc (-> html hc/parse hc/as-hickory)
          list-msg-raw (->> xdoc
                            (hs/select
                             (hs/descendant
                              (hs/and
                               (hs/tag :div)
                               (hs/attr :class (fn [s] (re-find (re-pattern "alert") s))))))
                            first)
          div-cls (get-in list-msg-raw [:attrs :class])
          alert-type (cond
                       (re-find #"alert-danger" (str div-cls)) "error"
                       (re-find #"alert-success" (str div-cls)) "success"
                       :default nil)
          message (some-> list-msg-raw :content last s/trim)
          ]
      {:alertType alert-type
       :message message})))

;; CREATE PARAM
(defn create-loginadmin-param
  [username password]
  {:form-params
   {"username" username
    "password" password}})

(defn create-register-member-param
  [m]
  {:form-params
   {"email" (:email m)
    "password" (:password m)
    "repassword" (:repassword m)
    "firstname" (:firstname m)
    "lastname" (:lastname m)
    "gender" (:gender m)
    "dob" (:dob m)
    "phone" (:phone m)}})

(defn create-order-seminar-param
  [m]
  {:form-params
   {"email_member" (:email m)
    "seminar_id" (:seminar_id m)}})


