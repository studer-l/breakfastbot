(ns breakfastbot.handlers.sign-off
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.handlers.common :refer [answers
                                                  person-date-matcher]]
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
  (let [result (db-ops/safe-remove db/db who when next-date)]
    (cond
      (= result :ok) (:ok-unhappy answers)
      (= result :ok-cancel) ((:cancel answers) next-date)
      (= result :no-signup) (:error-already-signed-off answers)
      (= result :no-event) (:error-no-event answers)
      (= result :no-member) (:error-no-member answers)
      (= (first result) :ok-new-responsible) ((:change-responsible answers)
                                              (second result)))))

(def sign-off-handler {:matcher parse-sign-off
                       :action (fn [{who :who when :when}]
                                 (sign-off who when (next-monday)))
                       :help (str "\"@**breakfastbot** cannot [me|email] [date]\""
                                  " -- Sign (yourself) off from (next) event")})
