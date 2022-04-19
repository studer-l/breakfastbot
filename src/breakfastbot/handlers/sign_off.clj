(ns breakfastbot.handlers.sign-off
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.db :as db]
            [breakfastbot.config :refer [config]]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.handlers.common :refer [answers person-date-matcher
                                                  change-bringer-reply
                                                  changed-bringer?]]
            [clojure.tools.logging :refer [info debug]]
            [java-time :as jt]))

(defn parse-sign-off
  "syntax: cannot [me | other] [date]; note that only ordinary emails are
  supported"
  [author message]
  (person-date-matcher
   #"^cannot\W*(?<who>me|\S+\@\S+\.\S+)?\W*(?<when>\d{1,2}\.\d{1,2}\.\d{4})?$"
   author message))

(defn sign-off
  [who when next-date]
  ;; ensure that we primed until this date...
  (debug "performing sign-off for" who "on" (jt/format when)
         "considering next breakfast is on" (jt/format next-date))
  (db-ops/prime-attendance db/db when)
  (let [nb-bringers (get-in config [:bot :nb-bringers])
        res (db-ops/safe-remove db/db who when next-date nb-bringers)]
    (cond
      (= res :ok)            {:direct-reply (str (:ok-unhappy answers) " " ((:eliza-reply answers) "i cannot come"))
                              :update       true}
      (= res :ok-cancel)     {:direct-reply ((:cancel answers) next-date)
                              :update       true}
      (= res :no-signup)     {:direct-reply (:error-signed-off answers)}
      (= res :no-event)      {:direct-reply (:error-no-event answers)}
      (= res :no-member)     {:direct-reply (:error-no-member answers)}
      (changed-bringer? res) (change-bringer-reply (second res)))))

(def sign-off-handler {:matcher parse-sign-off
                       :action (fn [{who :who when :when}]
                                 (sign-off who when (next-monday)))
                       :help (str "\"@**breakfastbot** cannot [me|email] [date]\""
                                  " -- Sign (yourself) off from (next) event")})
