(ns seminar.scraping
  (:require [clojure.tools.logging :as log]
            [myapp.util :as u]
            [seminar.parse.scraping :as sp]
            [org.httpkit.client :as http]
            [clojure.string :as s]))


(defn login-admin []
  (let [hc-params (:hc-params sp/config)
        {:keys [status headers body error] :as resp}
        @(http/get (get-in sp/config [:urls :login-admin]) hc-params)

        hc-params (assoc hc-params
                         :headers {"Referer" (get-in sp/config [:urls :login-admin])})
        cookie-map (u/get-cookie headers)
        admin-username (get-in seminar.config/credentials [:admin :username])
        admin-password (get-in seminar.config/credentials [:admin :password])
        
        login-param (sp/create-loginadmin-param admin-username admin-password)
        
        {:keys [status headers body error] :as resp}
        @(http/post (get-in sp/config [:urls :do-login-admin])
                    (u/assoc-cookie cookie-map
                                    (merge hc-params login-param)))
        _ (when-not (= 302 status)
            (throw (Exception. (str "Invalid login - username:" admin-username))))
        
        cookie-map (merge cookie-map (u/get-cookie headers))
        
        {:keys [status headers body error] :as resp}
        @(http/get (get-in sp/config [:urls :dashboard])
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
        @(http/get (get-in sp/config [:urls :dashboard])
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
      (log/debug "Already logged in"))
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
        @(http/get url
                   (u/assoc-cookie cookie-map hc-params))
        cookie-map (merge cookie-map (u/get-cookie headers))]
    {:cookies cookie-map
     :resp resp}))


(defn seminar*
  [state]
  (ensure-logged-in-admin state)
  (let [url-seminar (get-in sp/config [:urls :seminar-admin])
        data-seminar (perform-action-get-seminar state url-seminar)
        pagination (sp/parse-pagination (get-in data-seminar [:resp :body]))
        result (sp/parse-seminar-admin (get-in data-seminar [:resp :body]))]
    
    (if pagination
      (let [result' (loop [i (count pagination)
                           result []]
                      (if (zero? i)
                        result
                        (recur (dec i)
                               (let [data-seminar' (perform-action-get-seminar state (get pagination (dec i)))
                                     r1 (sp/parse-seminar-admin (get-in data-seminar' [:resp :body]))]
                                 (conj result r1)))))
            result' (->> result'
                         (conj result)
                         flatten vec)]
        result')
      result)))