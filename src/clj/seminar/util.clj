(ns seminar.util
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s])
  (:gen-class))


(defn string->number [str]
  (try
    (if (string? str)
      (read-string str)
      str)
    (catch Throwable e
      str)))

(defn get-cookie [headers]
  (some-> headers
          :set-cookie
          (s/split #",")
          (as-> xs
              (->> xs
                   (filter not-empty)
                   (map #(-> % (s/split #"=" 2) (update 0 keyword)))
                   (into {})))))

(defn assoc-cookie [cookie-map headers]
  "Assoc given cookie-map to headers."
  (let [cookie-seq (map (fn [[k v]]
                          (str (name k)
                               "="
                               (first
                                (s/split v #";"))))
                        cookie-map)]
    (merge-with merge headers {:headers {"Cookie" (s/join "; " cookie-seq)}})))

(defn merge-cookie [cookie-map headers]
  "Merge cookie-map with cookie found in headers.
   Internally use `get-cookie` to get the cookie in the headers."
  (merge cookie-map (get-cookie headers)))

