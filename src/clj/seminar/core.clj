(ns seminar.core
  (:require [seminar.handler :as handler]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [clojure.tools.nrepl.server :as nrepl-server]
            [luminus.http-server :as http]
            [seminar.config :refer [env]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [seminar.worker_pool :as sw])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop}
                http-server
                :start
                (http/start
                  (-> env
                      (assoc :handler (handler/app))
                      (update :port #(or (-> env :options :port) %))))
                :stop
                (http/stop http-server))

(mount/defstate ^{:on-reload :noop}
  nrepl-server
  :start
  (when-let [nrepl-port (env :nrepl-port)]
    (nrepl-server/start-server :port nrepl-port
                               :handler (apply nrepl-server/default-handler
                                               (map resolve
                                                    (remove #{'cider.nrepl.middleware.out/wrap-out}
                                                            cider.nrepl/cider-middleware)))
                               :bind "0.0.0.0"))
  :stop
  (when nrepl-server
    (nrepl-server/stop-server nrepl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (sw/initialize-all-default-workers 20)
  (start-app args))
