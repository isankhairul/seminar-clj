(ns seminar.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]))

(defstate env :start (load-config
                       :merge
                       [(args)
                        (source/from-system-props)
                        (source/from-env)]))


(def credentials
  (->> "credentials.edn"
       clojure.java.io/resource
       slurp
       read-string))
