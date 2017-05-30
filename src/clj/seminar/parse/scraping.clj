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
      :mahasiswa (str base-url "/mahasiswa")
      :seminar-admin (str base-url "/seminar-admin")

      ;; frontend
      :login (str base-url "/login")
      :do-login (str base-url "/front/mahasiswa/mahasiswa_login")
      :order-seminar (str base-url "/front/seminar/submit_order")}
     

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
                              (> count-content 1) (some->> tag-td
                                                           (hs/select
                                                            (hs/tag :a))
                                                           last
                                                           :attrs :id_delete_seminar)
                              (-> content :content first boolean) (-> content :content first)
                              :else content)]
                [key content]))
            [:no :temaSeminar :jadwalSeminar :pembicaraSeminar :tempatSeminar :kuotaSeminar
             :sisaKuota :kelasSeminar :semesterSeminar :pesertaSeminar :statusSeminar :idSeminar])
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
                           (hs/attr :class (fn [s] (= s "table table-bordered table-hover"))))
                          (hs/tag :tbody)
                          (hs/tag :tr))))
          
          result (some->> table-tr
                          (mapv parse-seminar-admin-table-tr))]
      result)))



;; CREATE PARAM
(defn create-loginadmin-param
  [username password]
  {:form-params
   {"username" username
    "password" password}})


