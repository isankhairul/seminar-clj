(ns seminar.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [seminar.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[seminar started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[seminar has shut down successfully]=-"))
   :middleware wrap-dev})
