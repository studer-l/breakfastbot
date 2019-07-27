(ns breakfastbot.handlers.sign-on
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.handlers.common :refer [answers person-date-matcher]]
            [clojure.tools.logging :refer [info debug]]
            [java-time :as jt]))

;; very similar to sign-off
(defn parse-sign-on
  "syntax: can [me | other] [date]; note that only ordinary emails are
  supported"
  [author message]
  (person-date-matcher
   #"^can\W*(?<who>me|\S+\@\S+\.\S+)?\W*(?<when>\d{1,2}\.\d{1,2}\.\d{4})?$"
   author message))

(defn- no-event
  [db date]
  (not (:exists (db/any-attendance-on-date db {:day date}))))

(defn- no-member
  [db email]
  (nil? (db/get-member-by-email db {:email email})))

(defn- do-sign-on
  "Perform actual sign-on if date + email checked out; Returns response"
  [who when]
  (db/insert-attendance-by-email db/db {:email who :day when})
  (debug "sign-on action succeeded")
  {:direct-reply (:ok-happy answers)
   :update       true})

(defn sign-on
  [who when]
  ;; ensure that we primed until this date...
  (debug "performing sign-on for" who "on" (jt/format when))
  (db-ops/prime-attendance db/db when)
  (cond
    (no-event db/db when) {:direct-reply (:error-no-event answers)}
    (no-member db/db who) {:direct-reply (:error-no-member answers)}
    :else                 (do-sign-on who when)))

(def sign-on-handler {:matcher parse-sign-on
                      :action (fn [{who :who when :when}] (sign-on who when))
                      :help (str "\"@**breakfastbot** can [me|email] [date]\""
                                 " -- Sign (yourself) up for (next) event")})
