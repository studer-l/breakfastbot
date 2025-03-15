(ns breakfastbot.handlers.deactivate
  (:require [breakfastbot.db :as db]
            [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.handlers.common :refer [answers change-bringer-reply
                                                  changed-bringer?]]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug]]
            [breakfastbot.config :refer [config]]
            [breakfastbot.db-ops :as db-ops]))

(defn parse-deactivate-member
  [_ message]
  (when (re-matches #"^deactivate \S+\@\S+\.\S+$" message)
    (subs message 11)))

(defn deactivate-member [next-date email]
  ;; check whether this email is known
  (jdbc/with-db-transaction [db-con db/db]
    (if (db-ops/no-such-member? db-con email)
      ;; Member did not exist
      {:direct-reply (:error-no-member answers)}
      (do
        ;; mark as deactivated
        (db/change-member-active db-con {:email email :active false})
        ;; sign off from the next breakfast, which may cause scheduling changes
        (let [nb-bringers (get-in config [:bot :nb-bringers])
              result (db-ops/safe-remove db-con email next-date next-date nb-bringers)]
          (debug "Result from safe-remove: " result)
          ;; sign-off from all other currently primed breakfasts
          (db/remove-attendances-from db-con {:email email :date next-date})
          (cond
            (= result :ok-cancel)     {:direct-reply ((:cancel answers) next-date)
                                       :update       true}
            (changed-bringer? result) (change-bringer-reply (second result))
            :otherwise                {:direct-reply (:ok-unhappy answers)
                                       :update       true}))))))

(def deactivate-handler {:matcher parse-deactivate-member
                         :action  (fn [who]
                                    (deactivate-member (next-monday) who))
                         :help    (str "\"@**breakfastbot** deactivate email\""
                                       " -- Deactivate member by email")})
