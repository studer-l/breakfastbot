(ns breakfastbot.handlers.reactivate
  (:require [breakfastbot.db :as db]
            [breakfastbot.handlers.common :refer [answers]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug]]
            [breakfastbot.db-ops :as db-ops]))


(defn parse-reactivate-member
  [_ message]
  (if (re-matches #"^reactivate \S+\@\S+\.\S+$" message)
    (subs message 11)))

(defn reactivate-member [email]
  ;; check whether this email is known
  (jdbc/with-db-transaction [db-con db/db]
    (if (db-ops/no-such-member? db-con email)
      ;; Member did not exist
      {:direct-reply (:error-no-member answers)}
      ;; check if already active
      (if (db-ops/is-active? db/db email)
        {:direct-reply (:error-active answers)}
        (do
          (debug "Re-activating member" email)
          (db/change-member-active db-con {:email email :active true})
          ;; re-prime
          (db-ops/prime-attendance db-con)
          {:direct-reply (:ack answers)
           :notification {:who     email
                          :message (:reactivate answers)}
           :update       true})))))

(def reactivate-handler {:matcher parse-reactivate-member
                         :action  reactivate-member
                         :help    (str "\"@**breakfastbot** reactive email\""
                                       " -- Reactivate member by email")})
