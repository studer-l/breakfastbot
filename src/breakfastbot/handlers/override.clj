(ns breakfastbot.handlers.override
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.handlers.common :refer [answers person-date-matcher]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [info debug]]))

;; handler to override bringer on a given date

(defn parse-override
  "syntax: override [me | other] [date]"
  [author message]
  (person-date-matcher
   #"^override\W*(?<who>me|\S+\@\S+\.\S+)?\W*(?<when>\d{1,2}\.\d{1,2}\.\d{4})?$"
   author message))

(defn override-bringer
  [who when]
  (jdbc/with-db-transaction [tx db/db]
    ;; ensure that we primed until this date...
    (debug "performing sign-on for" who "on" when)
    (db-ops/prime-attendance tx when)

    ;; is this a valid event at all?
    (if-not (:exists (db/any-attendance-on-date tx {:day when}))
      (:error-no-event answers)
      (do
        (if (db/have-bringer-for-day tx {:day when})
          (db/change-bringer-on tx {:day when :email who})
          (db/set-bringer-by-email tx {:day when :email who}))
        (:ack answers)))))

(def override-bringer-handler
  {:matcher parse-override
   :action (fn [{who :who when :when}] (override-bringer who when))
   :help (str "\"@**breakfastbot** override [me|email] [date]\""
              " -- Override duty bringer by assigning it to "
              "(you) for (next) event")})
