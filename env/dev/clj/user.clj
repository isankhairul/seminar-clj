(ns user
  (:require [mount.core :as mount]
            seminar.core))

(defn start []
  (mount/start-without #'seminar.core/nrepl-server))

(defn stop []
  (mount/stop-except #'seminar.core/nrepl-server))

(defn restart []
  (stop)
  (start))


