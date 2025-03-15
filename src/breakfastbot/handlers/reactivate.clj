(ns breakfastbot.handlers.reactivate
  (:require [breakfastbot.db :as db]
            [breakfastbot.handlers.common :refer [answers]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug]]
            [breakfastbot.db-ops :as db-ops]
            [java-time :as jt]))

(defn parse-reactivate-member
  [_ message]
  (if (re-matches #"^reactivate \S+\@\S+\.\S+$" message)
    (subs message 11)))

(defn reactivate-member
  "Reactivates member with given email, signing them up for all events in range
  `prime-from` to `prime-until`, typically today until current
  `attendance_primed_until` value in app-state table"

  ([email]
   (jdbc/with-db-transaction [db-con db/db]
     (reactivate-member db-con email (jt/local-date)
                        (db-ops/currently-primed db-con))))

  ([email prime-from prime-until]
   (jdbc/with-db-transaction [db-con db/db]
     (reactivate-member db-con email prime-from prime-until)))

  ([db-con email prime-from prime-until]
   ;; check whether this email is known
   (if (db-ops/no-such-member? db-con email)
     ;; Member did not exist
     {:direct-reply (:error-no-member answers)}
     ;; check if already active
     (if (db-ops/is-active? db-con email)
       {:direct-reply (:error-active answers)}
       (do
         (debug "Re-activating member" email)
         (db/change-member-active db-con {:email email :active true})
         ;; re-prime
         (db-ops/prime-single-member-attendance-email db-con prime-from
                                                      prime-until email)
         {:direct-reply (:ack answers)
          :notification {:who     email
                         :message (:reactivate answers)}
          :update       true})))))

(def reactivate-handler {:matcher parse-reactivate-member
                         :action  reactivate-member
                         :help    (str "\"@**breakfastbot** reactivate email\""
                                       " -- Reactivate member by email")})
