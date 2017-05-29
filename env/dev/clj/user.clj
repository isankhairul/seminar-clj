(ns user
  (:require [mount.core :as mount]
            seminar.core))

(defn start []
  (mount/start-without #'seminar.core/repl-server))

(defn stop []
  (mount/stop-except #'seminar.core/repl-server))

(defn restart []
  (stop)
  (start))


