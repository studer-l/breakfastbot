(ns breakfastbot.handlers.deactivate
  (:require [breakfastbot.db :as db]
            [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.handlers.common :refer [answers]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug]]
            [breakfastbot.db-ops :as db-ops]))

(defn parse-deactivate-member
  [_ message]
  (if (re-matches #"^deactivate \S+\@\S+\.\S+$" message)
    (subs message 11)))

(defn- member-exists? [db-con email]
  (-> db-con (db/get-member-by-email {:email email}) some?))

(defn deactivate-member [next-date email]
  ;; check whether this email is known
  (jdbc/with-db-transaction [db-con db/db]
    (if-not (member-exists? db-con email)
      ;; Member did not exist
      (:error-no-member answers)
      (do
        ;; mark as deactivated
        (db/change-member-active db-con {:email email :active false})
        ;; sign off from the next breakfast, which may cause scheduling changes
        (let [result (db-ops/safe-remove db-con email next-date next-date)]
          (debug "Result from safe-remove: " result)
          ;; sign-off from all other currently primed breakfasts
          (db/remove-attendances-from db-con {:email email :date next-date})
          (cond
            (= result :ok-cancel) ((:cancel answers) next-date)
            (and (seqable? result) (= (first result) :ok-new-responsible))
            ((:change-responsible answers) (second result))
            :otherwise (:ok-unhappy answers)))))))

(def deactivate-handler {:matcher parse-deactivate-member
                         :action (fn [who]
                                   (deactivate-member (next-monday) who))
                         :help (str "\"@**breakfastbot** deactive email\""
                                    " -- Deactivate member by email")})

