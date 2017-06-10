(ns seminar.scraping
  (:require [clojure.tools.logging :as log]
            [seminar.util :as u]
            [seminar.parse.scraping :as sp]
            [seminar.config :as cfg]
            [org.httpkit.client :as http]
            [clojure.string :as s]))

(defn api-action
  [method url opts & [n]]
  (try
    (let [result @(http/request
                   (merge {:method method :url (str url)} opts))]
      result)

    (catch Throwable e
      (log/debug "ERROR-ACTION" (pr-str [url (.getMessage e)]))

      ;; max retry 2 times
      (when (> (or n 0) 1)
        (throw (Exception. (str "Failed request api-action after 2 times. " url))))

      ;; callback when timeout
      (api-action method url opts ((fnil inc 0) n)))))


(defn login-admin []
  (let [hc-params (:hc-params sp/config)
        
        {:keys [status headers body error] :as resp}
        (api-action :get (get-in sp/config [:urls :login-admin]) hc-params)

        hc-params (assoc hc-params
                         :headers {"Referer" (get-in sp/config [:urls :login-admin])})
        cookie-map (u/get-cookie headers)
        admin-username (get-in cfg/credentials [:admin :username])
        admin-password (get-in cfg/credentials [:admin :password])
        
        login-param (sp/create-loginadmin-param admin-username admin-password)
        
        {:keys [status headers body error] :as resp}
        (api-action :post (get-in sp/config [:urls :do-login-admin])
                    (u/assoc-cookie cookie-map
                                    (merge hc-params login-param)))
        
        _ (when-not (= 302 status)
            (throw (Exception. (str "Invalid login - username:" admin-username))))
        
        cookie-map (merge cookie-map (u/get-cookie headers))
        
        {:keys [status headers body error] :as resp}
        (api-action :get (get-in sp/config [:urls :dashboard])
                    (u/assoc-cookie cookie-map hc-params))
        
        cookie-map (merge cookie-map (u/get-cookie headers))]
    
    {:cookies cookie-map}))


(defn logged-in-admin?
  [state-map]
  (let [hc-params (:hc-params sp/config)
        cookie-map (get-in state-map [:session-ctx :cookies])
        hc-params (assoc hc-params
                         :headers {"Referer" (get-in sp/config [:urls :login-admin])})
        {:keys [status headers body error] :as resp}
        (api-action :get (get-in sp/config [:urls :dashboard])
                    (u/assoc-cookie cookie-map hc-params))
        
        user-menu? (sp/parse-loggedadmin body)
        location (:location headers)]
    
    (or user-menu?
        (not (some? location)))))


(defn ensure-logged-in-admin
  [state & [n]]
  (assert (> 5 (or n 0)) "Failed login after 5 times")
  (try
    (if-not (and (:session-ctx @state)
                 (logged-in-admin? @state))
      (swap! state assoc :session-ctx (login-admin))
      (log/debug "Already logged in" true))
    (catch Throwable e
      (log/debug "Login fail" (.getMessage e))
      (ensure-logged-in-admin state ((fnil inc 0) n)))))


(defn perform-action-actual-get-seminar
  [state url]
  (log/debug "URL-GET-SEMINAR" url)
  (let [hc-params (:hc-params sp/config)
        hc-params (assoc hc-params
                         :headers {"Referer" (get-in sp/config [:urls :dashboard])})
        
        cookie-map (get-in @state [:session-ctx :cookies])
        {:keys [status headers body error] :as resp}
        (api-action :get url (u/assoc-cookie cookie-map hc-params))
        
        cookie-map (merge cookie-map (u/get-cookie headers))]
    {:cookies cookie-map
     :resp resp}))

(defn perform-action-actual-get-member
  [state url]
  (log/debug "URL-GET-MEMBER" url)
  (let [hc-params (:hc-params sp/config)
        hc-params (assoc hc-params
                         :headers {"Referer" (get-in sp/config [:urls :dashboard])})
        
        cookie-map (get-in @state [:session-ctx :cookies])
        {:keys [status headers body error] :as resp}
        (api-action :get url (u/assoc-cookie cookie-map hc-params))
        
        cookie-map (merge cookie-map (u/get-cookie headers))]
    {:cookies cookie-map
     :resp resp}))

(defn perform-action-actual-get-peserta
  [state url]
  (log/debug "URL-GET-PESERTA" url)
  (let [hc-params (:hc-params sp/config)
        hc-params (assoc hc-params
                         :headers {"Referer" (get-in sp/config [:urls :dashboard])})
        
        cookie-map (get-in @state [:session-ctx :cookies])
        {:keys [status headers body error] :as resp}
        (api-action :get url (u/assoc-cookie cookie-map hc-params))
        
        cookie-map (merge cookie-map (u/get-cookie headers))]
    {:cookies cookie-map
     :resp resp}))

(defn perform-action-actual-register-member
  [params]
  (let [state (atom {})
        _ (ensure-logged-in-admin state)
        urls (get-in sp/config [:urls])
        hc-params (:hc-params sp/config)
                
        cookie-map (get-in @state [:session-ctx :cookies])
        {:keys [status headers body error] :as resp}
        (api-action :get (:login-front urls)
                    (u/assoc-cookie cookie-map hc-params))
        
        cookie-map (merge cookie-map (u/get-cookie headers))

        register-member-param (sp/create-register-member-param params)
        
        {:keys [status headers body error] :as resp}
        (api-action :post (:submit-register-member urls)
                    (merge register-member-param
                           (u/assoc-cookie cookie-map hc-params)))
        
        cookie-map (merge cookie-map (u/get-cookie headers))
        
        error-message (if (= 200 status)
                        (sp/parse-has-error body)
                        [])
        
        {:keys [status headers body error] :as resp}
        (api-action :get (:login-front urls)
                    (u/assoc-cookie cookie-map hc-params))
        
        cookie-map (merge cookie-map (u/get-cookie headers))

        parse-alert (sp/parse-alert-message body)

        error-message (if (= "error" (:alertType parse-alert))
                        (conj error-message
                              (:message parse-alert))
                        error-message)
        success-message (when (= "success" (:alertType parse-alert))
                          (:message parse-alert))
        ]
    {:cookies cookie-map
     :resp resp
     :errorMessage error-message
     :successMessage success-message}
    ))

(defn perform-action-actual-order-seminar
  [state params]
  (let [cookie-map (get-in @state [:session-ctx :cookies])

        urls (get-in sp/config [:urls])
        hc-params (:hc-params sp/config)
        
        order-seminar-param (sp/create-order-seminar-param params)

        {:keys [status headers body error] :as resp}
        (api-action :post (:submit-order-seminar urls)
                    (merge order-seminar-param
                           (u/assoc-cookie cookie-map hc-params)))
        
        cookie-map (merge cookie-map (u/get-cookie headers))

        resp-message (cheshire.core/parse-string body true)]
    
    {:resp resp
     :respMessage resp-message
     :cookies cookie-map}))

(defn perform-action-actual-cetak-ticket
  [state params]
  (let [cookie-map (get-in @state [:session-ctx :cookies])

        urls (get-in sp/config [:urls])
        hc-params (assoc (:hc-params sp/config) :as :stream)

        url-cetak-ticket (str (:cetak-ticket urls) "/" (:order_id params))

        {:keys [status headers body error] :as resp}
        (api-action :get url-cetak-ticket (u/assoc-cookie cookie-map hc-params))
        
        filename-attach (some-> (re-find #"\"([^\"]*)\""
                                         (str (:content-disposition headers)))
                                last
                                (s/replace #"\ " ""))
        
        filename (str "/tmp/" filename-attach)]
    ;;save into temporaryfile
    (clojure.java.io/copy body (java.io.File. filename))
    
    filename))

;; Main
(defn perform-get-seminar
  [state params]
  (ensure-logged-in-admin state)
  (let [url-seminar (get-in sp/config [:urls :seminar-admin])
        data-seminar (perform-action-actual-get-seminar state url-seminar)
        pagination (sp/parse-pagination (get-in data-seminar [:resp :body]))
        result (sp/parse-seminar-admin (get-in data-seminar [:resp :body]))]
    
    (if pagination
      (let [result' (loop [i (count pagination)
                           result []]
                      (if (zero? i)
                        result
                        (recur (dec i)
                               (let [data-seminar' (perform-action-actual-get-seminar state (get pagination (dec i)))
                                     r1 (sp/parse-seminar-admin (get-in data-seminar' [:resp :body]))]
                                 (conj result r1)))))
            result' (->> result'
                         (conj result)
                         flatten vec)]
        result')
      result)))

(defn perform-get-member
  [state params]
  (ensure-logged-in-admin state)
  (let [url-member (get-in sp/config [:urls :member])
        data-member (perform-action-actual-get-member state url-member)
        pagination (sp/parse-pagination (get-in data-member [:resp :body]))
        result (sp/parse-member-admin (get-in data-member [:resp :body]))]
    
    (if pagination
      (let [result' (loop [i (count pagination)
                           result []]
                      (if (zero? i)
                        result
                        (recur (dec i)
                               (let [data-member' (perform-action-actual-get-member state (get pagination (dec i)))
                                     r1 (sp/parse-member-admin (get-in data-member' [:resp :body]))]
                                 (conj result r1)))))
            result' (->> result'
                         (conj result)
                         flatten vec)]
        result')
      result)))

(defn perform-register-member
  [params]
  (perform-action-actual-register-member params))

(defn perform-order-seminar
  [state params]
  (ensure-logged-in-admin state)
  (-> (perform-action-actual-order-seminar state params)
      :respMessage))

(defn perform-get-peserta
  [state params]
  (ensure-logged-in-admin state)
  (let [state-map @state
        urls (get-in sp/config [:urls])
        {:keys [resp cookies]} (perform-action-actual-get-member state (:member urls))

        state-map (update-in state-map [:session-ctx :cookies]
                             (fn [st] (merge st cookies)))
        url-peserta (str (:list-peserta urls) "/" (:seminar_id params))
        data-peserta (perform-action-actual-get-peserta (atom state-map) url-peserta)

        pagination (sp/parse-pagination (get-in data-peserta [:resp :body]))
        result (sp/parse-peserta-admin (get-in data-peserta [:resp :body]))]
    
    (if pagination
      (let [result' (loop [i (count pagination)
                           result []]
                      (if (zero? i)
                        result
                        (recur (dec i)
                               (let [data-peserta' (perform-action-actual-get-peserta
                                                    state (get pagination (dec i)))
                                     r1 (sp/parse-peserta-admin (get-in data-peserta' [:resp :body]))]
                                 (conj result r1)))))
            result' (->> result'
                         (conj result)
                         flatten vec)]
        result')
      result)
    ))

(defn perform-cetak-ticket
  [state params]
  (ensure-logged-in-admin state)
  (perform-action-actual-cetak-ticket state params))