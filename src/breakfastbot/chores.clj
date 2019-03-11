(ns breakfastbot.chores
  (:require [clojure.core.async :as a]
            [clojure.tools.logging :refer [info]]
            [mount.core :refer [defstate]]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.announcement :refer [announce-breakfast]]
            [java-time :as jt]))

(defn- repeatedly-async-call
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

(def hour-in-millis (* 1000 60))
(def day-in-millis (* hour-in-millis 24))

(defstate attendance-prime-task
  :start (repeatedly-async-call day-in-millis db-ops/prime-attendance)
  :stop (a/>!! :stop attendance-prime-task))

(defstate announce-breakfast-task
  :start (repeatedly-async-call hour-in-millis (fn [] (announce-breakfast (jt/local-date-time))))
  :stop (a/>!! :stop announce-breakfast-task))
