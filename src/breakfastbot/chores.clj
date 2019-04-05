(ns breakfastbot.chores
  (:require [breakfastbot.announcement :refer [announce-breakfast]]
            [breakfastbot.db :refer [db]]
            [breakfastbot.db-ops :as db-ops]
            [clojure.core.async :as a]
            [clojure.tools.logging :refer [info]]
            [java-time :as jt]
            [mount.core :refer [defstate]]))

(defn repeatedly-async-call
  [msecs func]
  (let [kill-channel (a/chan)]
    (a/go-loop []
      (let [[_ channel] (a/alts! [kill-channel (a/timeout msecs)]
                                 :priority true)]
        (cond
          (= channel kill-channel) (info "Stopping reoccurring task")
          :if-timed-out (do
                          (func)
                          (recur)))))
    kill-channel))

(def minute-in-millis (* 1000 60))
(def hour-in-millis (* minute-in-millis 60))

(defstate attendance-prime-task
  :start (repeatedly-async-call hour-in-millis (fn [] db-ops/prime-attendance db))
  :stop (a/>!! :stop attendance-prime-task))

(defstate announce-breakfast-task
  :start (repeatedly-async-call minute-in-millis (fn [] (announce-breakfast (jt/local-date-time))))
  :stop (a/>!! :stop announce-breakfast-task))
