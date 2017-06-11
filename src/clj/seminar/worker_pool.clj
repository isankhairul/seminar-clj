(ns seminar.worker_pool
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :as async]))

(defonce -counter (atom 0))

(def worker-pool
  (atom {}))

(defonce ^:dynamic *MAX-WORKER 100)

(defn init-workers*
  [name pool size]
  (let [workers (map (fn [id] (atom {:id (swap! -counter inc)}))
                     (range size))
        ch (async/chan (async/sliding-buffer *MAX-WORKER))]
    (doseq [w workers]
      (async/>!! ch w))
    (swap! pool assoc name ch)))

(defn take-worker*
  [name pool & [t]]
  (if (number? t)
    (some-> (get @pool name)
            (cons [(async/timeout t)])
            vec
            (async/alts!! :priority true)
            first)
    (some-> (get @pool name)
            (async/<!!))))

(defn give-worker*
  [name pool worker]
  (some-> (get @pool name)
          (async/>!! worker)))

(defn take-worker
  [name & [t]]
  (let [worker (take-worker* name worker-pool 5)]
    (assert worker (str "No worker for " name))
    worker))

(defn give-worker
  [name worker]
  (give-worker* name worker-pool worker))

(defn initialize-workers
  [name n]
  (println "Initializing worker-pool for" name "with" n "workers.")
  (init-workers* name worker-pool n))

(defn initialize-all-default-workers
  ([] (initialize-all-default-workers 20))
  ([n]
   (let [n (or n 20)
         workers [["seminar" n]
                  ["seminar-scheduler" n]]]
     (doseq [[name i] workers]
       (initialize-workers name n)))))


(defn get-standby-workers-count []
  (->> @worker-pool
       (map
        (fn [[k v]]
          [k {:count (count (.. v buf buf))
              :details (map #(-> % deref (dissoc "session-ctx"))
                            (.. v buf buf))}]))))

