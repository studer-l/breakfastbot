(ns breakfastbot.handlers.override
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.handlers.common :refer [answers person-date-matcher]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug]]))

;; handler to override bringer on a given date

(defn parse-override
  "syntax: override email+ [date]"
  [author message]
  (let [re #"^override\W*(?<who>(\S+\@\S+\.\S+\W*)+)(?<when>\d{1,2}\.\d{1,2}\.\d{4})?$"
        {date :when who :who} (person-date-matcher re author message)]
    (when who
      ;; split 'who' if required
      {:when date :who (re-seq #"\S+" who)})))

(defn override-bringer
  [emails when]
  (jdbc/with-db-transaction [tx db/db]
    (debug "overriding bringers to" emails "on" when)
    ;; ensure that we primed until this date...
    (db-ops/prime-attendance tx when)

    ;; is this a valid event at all?
    (if-not (:exists (db/any-attendance-on-date tx {:day when}))
      {:direct-reply (:error-no-event answers)}
      (do
        (db/reset-bringer-for-day db/db {:day when})
        (doseq [email emails]
          (debug "setting bringer" email)
          (db/set-bringer-by-email db/db {:day when :email email}))
        (debug "override successful")
        {:direct-reply (:ack answers) :update true}))))

(def override-bringer-handler
  {:matcher parse-override
   :action (fn [{who :who when :when}] (override-bringer who when))
   :help (str "\"@**breakfastbot** override email+ [date]\""
              " -- Override bringer duty by assigning it to "
              "email(s) for (next) event")})
