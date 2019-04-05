(ns breakfastbot.handlers.sign-off
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.handlers.common :refer [answers
                                                  person-date-matcher]]
            [clojure.tools.logging :refer [info debug]]))

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
  (debug "performing sign-off for" who "on" when
         "considering next breakfast is on" next-date)
  (db-ops/prime-attendance db/db when)
  (let [was-supposed-to-bring
        (and (= when next-date)
             (= who (:email (db/get-bringer-on db/db {:day next-date}))))]
    ;; if was supposed to bring, remove bringer state
    (if was-supposed-to-bring (db/reset-bringer-for-day db/db {:day when}))
    (if (zero? (db/remove-attendance-by-email-at db/db {:day when :email who}))
      ;; ... either user typo and there's no event, or there is no breakfast on
      ;; this date, but which is it?!
      (if (:exists (db/any-attendance-on-date db/db {:day when}))
        (:error-already-signed-off answers)
        (:error-no-event answers))
      ;; otherwise we did remove the user from the event
      (if was-supposed-to-bring
        ;; figure out who is now responsible
        (if-let [{email :email} (db-ops/choose-bringer db/db when)]
          ((:change-responsible answers) email)
          (:cancel answers))
        (:ok-unhappy answers)))))

(def sign-off-handler {:matcher parse-sign-off
                       :action (fn [{who :who when :when}]
                                 (sign-off who when (next-monday)))
                       :help (str "\"@**breakfastbot** cannot [me|email] [date]\""
                                  " -- Sign (yourself) off from (next) event")})
