(ns breakfastbot.db-ops
  (:require [breakfastbot.date-utils :refer [next-monday mondays]]
            [breakfastbot.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [info debug]]
            [java-time :as jt]))

(defn prime-breakfast
  "Signs up all currently active members for breakfast on given date"
  [date]
  (dorun
   (for [{id :id} (db/get-active-members db/db)]
     (db/insert-attendance-by-id db/db {:day date :id id}))))

(defn- currently-primed
  [] (-> db/db
         db/get-last-primed
         :attendance_primed_until
         jt/local-date))

(defn prime-attendance
  "Add people preemptively to the attendance table"
  ([dbspec] ;; default 30 days in the future
   (let [primed-from (jt/local-date)
         primed-to (jt/plus primed-from (jt/days 30))]
     (prime-attendance dbspec primed-to)))
  ([dbspec prime-until]
   (let [primed-from (currently-primed)
         primed-to prime-until]
     (info "Priming from"
           (jt/format "d.M.yyyy" primed-from)
           "until"
           (jt/format "d.M.yyyy" primed-to))
     (dorun (for [monday (mondays primed-from primed-to)]
              (prime-breakfast monday)))
     (db/set-last-primed dbspec {:date (jt/max primed-from primed-to)}))))

(defn add-new-team-member
  "They are signed up for all currently scheduled breakfasts and their bring
  date is set to today, so they wont have to bring breakfast for some time."
  [email fullname]
  (let [{new-id :id} (db/insert-member db/db {:email email :fullname fullname})
        primed-from (jt/local-date)
        primed-to (currently-primed)]
    (dorun (for [monday (mondays primed-from primed-to)]
             (db/insert-attendance-by-id db/db {:day monday
                                                :id new-id})))
    (info "Added new team member" fullname)))

(defn choose-bringer
  "Determines who should bring breakfast on a given date.
  First counts attendances and then updates bringer table.
  Returns bringer email and fullname if one is chosen, nil otherwise."
  [db date]
  (jdbc/with-db-transaction [tx db]
    (if-let [id (some->> {:day date}
                         (db/get-attendance-counts-since-bringing tx)
                         seq ;; transforms empty list to nil, discarding it
                         (apply max-key :count)
                         :id)]
      (do
        (db/set-bringer-on tx {:day date :id id})
        (info "bringer set for" (jt/format "d.M.yyyy" date) "to" id)
        (db/get-member-by-id tx {:id id})))))

(defn- get-or-choose-bringer
  [db date]
  (jdbc/with-db-transaction [tx db]
    (let [bringer (db/get-bringer-on tx {:day date})]
      (debug "Existing bringer id = " bringer)
      (if (nil? bringer)
        (do (debug "choosing new bringer")
            (choose-bringer tx date))
        bringer))))

(defn prepare-breakfast
  "Prepares next breakfast by selecting bringer and getting a list of attendees"
  [db date]
  ;; use the fact that nested transactions are absorbed by the outer transaction
  (jdbc/with-db-transaction [tx db]
    (let [bringer (get-or-choose-bringer tx date)
          attendees (into [] (db/get-all-attendees tx {:day date}))]
      (debug "bringer = " bringer ", attendees = " attendees)
      (if (not-any? nil? [bringer attendees])
        {:bringer bringer :attendees attendees}))))

(defn safe-remove
  "Remove `who` from event `when` considering next event on date `next-date`
  Returns one of:
  - `:ok`
  - `:ok-cancel` if no more attendees
  - `:ok-new-responsible` along with email of newly responsible person.
  - `:no-signup`
  - `:no-event`"
  [db-con who when next-date]
  (let [was-supposed-to-bring
        (and (= when next-date)
             (= who (:email (db/get-bringer-on db/db {:day next-date}))))]
    ;; if was supposed to bring, remove bringer state
    (if was-supposed-to-bring (db/reset-bringer-for-day db/db {:day when}))
    (if (zero? (db/remove-attendance-by-email-at db/db {:day when :email who}))
      ;; ... either user typo and there's no event, or there is no breakfast on
      ;; this date, but which is it?!
      (if (:exists (db/any-attendance-on-date db/db {:day when}))
        :no-signup
        :no-event)
      ;; otherwise we did remove the user from the event
      (if was-supposed-to-bring
        ;; figure out who is now responsible
        (if-let [{email :email} (choose-bringer db/db when)]
          [:ok-new-responsible email]
          :ok-cancel)
        :ok))))
