(ns breakfastbot.handlers.cancel
  (:require [clojure.string :as str]
            [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.handlers.common :as common]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.chatting :as chatting]))

(defn parse-cancel
  [_ message]
  (let [[matched date] (re-find #"^cancel\W*(?<when>\d{1,2}\.\d{1,2}\.\d{4})?$"
                                message)]
    (when matched
      ;; try to parse date
      (if date
        (common/try-parse-date date)
        (next-monday)))))


(defn cancel-action [date]
  (db-ops/cancel-event date)
  {:direct-reply (str (chatting/date->subject date) " is CANCELED!")
   :update       true})

(def cancel-handler {:matcher parse-cancel
                     :action  cancel-action
                     :help    (str "\"@**breakfastbot** cancel [date]\""
                                   " -- Cancels (next) event (d.M.yyyy),"
                                   " cannot be undone")})
