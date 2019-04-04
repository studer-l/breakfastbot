(ns breakfastbot.handlers.sign-on
  (:require [clojure.tools.logging :refer [info debug]]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.db :as db]
            [breakfastbot.handlers.common :refer [answers person-date-matcher]]))

;; very similar to sign-off
(defn parse-sign-on
  "syntax: can [me | other] [date]; note that only ordinary emails are
  supported"
  [author message]
  (person-date-matcher
   #"^can\W*(?<who>me|\S+\@\S+\.\S+)?\W*(?<when>\d{1,2}\.\d{1,2}\.\d{4})?$"
   author message))

(defn sign-on
  [who when]
  ;; ensure that we primed until this date...
  (debug "performing sign-on for" who "on" when)
  (db-ops/prime-attendance db/db when)
  ;; is this a valid event at all?
  (if-not (:exists (db/any-attendance-on-date db/db {:day when}))
    (:error-no-event answers)
    (do
      (db/insert-attendance-by-email db/db {:email who :day when})
      (:ok-happy answers))))

(def sign-on-handler {:matcher parse-sign-on
                      :action (fn [{who :who when :when}] (sign-on who when))
                      :help (str "\"@**breakfastbot** can [me|email] [date]\""
                                 " -- Sign (yourself) up for (next) event")})
