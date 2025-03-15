(ns breakfastbot.db-ops
  (:require [breakfastbot.date-utils :refer [next-monday mondays]]
            [breakfastbot.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [info debug]]
            [java-time :as jt])
  (:import (org.postgresql.util PSQLException)))

(defn prime-breakfast
  "Signs up all currently active members for breakfast on given date"
  [date]
  (dorun
   (for [{id :id} (db/get-active-members db/db)]
     (db/insert-attendance-by-id db/db {:day date :id id}))))

(defn is-active? [dbspec email]
  (-> dbspec (db/get-member-by-email {:email email}) :active))

(defn currently-primed
  ([] (currently-primed db/db))
  ([dbspec] (-> dbspec
                db/get-last-primed
                :attendance_primed_until
                jt/local-date)))

(defn prime-attendance
  "Add people preemptively to the attendance table"
  ([dbspec] ;; default 30 days in the future
   (let [primed-from (jt/local-date)
         primed-to   (jt/plus primed-from (jt/days 30))]
     (prime-attendance dbspec primed-to)))
  ([dbspec prime-until]
   (let [primed-from (currently-primed)
         primed-to   prime-until]
     (info "Priming from"
           (jt/format "d.M.yyyy" primed-from)
           "until"
           (jt/format "d.M.yyyy" primed-to))
     (dorun (for [monday (mondays primed-from primed-to)]
              (prime-breakfast monday)))
     (db/set-last-primed dbspec {:date (jt/max primed-from primed-to)}))))

(defn prime-single-member-attendance-id
  [dbspec prime-from prime-to id]
  (dorun (for [monday (mondays prime-from prime-to)]
           (db/insert-attendance-by-id dbspec {:day monday :id id}))))

(defn prime-single-member-attendance-email
  [dbspec prime-from prime-to email]
  (dorun (for [monday (mondays prime-from prime-to)]
           (db/insert-attendance-by-email dbspec {:day monday :email email}))))

(defn add-new-team-member
  "They are signed up for all currently scheduled breakfasts and their bring
  date is set to today, so they wont have to bring breakfast for some time."
  [email fullname]
  (debug "add-new-team-member" email fullname)
  (try
    (let [{new-id :id} (db/insert-member db/db {:email email :fullname fullname})
          prime-from   (jt/local-date)
          prime-to     (currently-primed)]
      (prime-single-member-attendance-id db/db prime-from prime-to new-id)
      (info "Added new team member" fullname)
      :success)
    (catch PSQLException _
      :error)))

(defn- debug-print [counts]
  (debug "attendance counts are " counts)
  counts)

(defn- choose-bringer-by-attendance-counts
  [tx date nb-bringers]
  (some->> {:day date}
           (db/get-attendance-counts-since-bringing tx)
           seq ;; transforms empty list to nil, discarding it
           ;; sort-by with id for tie-breaking
           (sort-by (fn [{:keys [count id]}] [(- count) id]))
           debug-print
           (map :id)
           (take nb-bringers)))

(defn choose-bringers
  "Determines who should bring breakfast on a given date.
  First counts attendances and then updates bringer table.
  Returns sequence of bringer email and fullname if any is chosen,
  nil otherwise."
  [db date nb-bringers]
  (jdbc/with-db-transaction [tx db]
    (when-let [ids (choose-bringer-by-attendance-counts tx date nb-bringers)]
      (info "bringers are" ids)
      (doall
       (for [id ids]
         (do (info "setting bringer for" (jt/format "d.M.yyyy" date) "to" id)
             (db/set-bringer-on tx {:day date :id id})
             (info "success")
             (db/get-member-by-id tx {:id id})))))))

(defn- get-or-choose-bringers
  [db date nb-bringers]
  (jdbc/with-db-transaction [tx db]
    (let [bringer (db/get-bringers-on tx {:day date})]
      (debug "Existing bringer id = " bringer)
      (if-not (seq bringer)
        (do (debug "choosing new bringers")
            (choose-bringers tx date nb-bringers))
        bringer))))

(defn prepare-breakfast
  "Prepares next breakfast by selecting bringer and getting a list of attendees"
  [db date nb-bringers]
  ;; use the fact that nested transactions are absorbed by the outer transaction
  (jdbc/with-db-transaction [tx db]
    (let [bringer   (get-or-choose-bringers tx date nb-bringers)
          attendees (vec (db/get-all-attendees tx {:day date}))]
      (debug "bringer = " bringer ", attendees = " attendees)
      (when (not-any? nil? [(seq bringer) (seq attendees)])
        {:bringer bringer :attendees attendees}))))

(defn no-such-member?
  "Returns true if where is no such member"
  [db email]
  (nil? (db/get-member-by-email db {:email email})))

(defn safe-remove
  "Remove `who`: email from event `when` considering next event on date
  `next-date`.
  Returns one of:
  - `:ok`
  - `:ok-cancel` if no more attendees
  - `:ok-new-responsible` along with email of newly responsible person.
  - `:no-signup`
  - `:no-event`
  - `:no-member`"
  [db-con who date next-date nb-bringers]
  (if (no-such-member? db-con who) :no-member
      (let [args {:day next-date :email who}
            was-supposed-to-bring (db/is-supposed-to-bring-on db-con args)]
        ;; if was supposed to bring, remove bringer state
        (if (zero? (db/remove-attendance-by-email-at db-con {:day   date
                                                             :email who}))
          ;; ... either user typo and there's no event, or there is no breakfast on
          ;; this date, but which is it?!
          (if (:exists (db/any-attendance-on-date db-con {:day date}))
            :no-signup
            :no-event)
          ;; otherwise we did remove the user from the event
          (if was-supposed-to-bring
            (do (debug "User" who "was supposed to bring breakfast on date"
                       (jt/format date))
                (db/reset-bringer-for-day db-con {:day date})
                ;; figure out who is now responsible
                (if-let [bringers (choose-bringers db-con date nb-bringers)]
                  [:ok-new-responsible (map :email bringers)]
                  :ok-cancel))
            :ok)))))

(defn attends-event? [email date]
  (some #(= email (:email %))
        (db/get-all-attendees db/db {:day date})))


(defn cancel-event [date]
  (jdbc/with-db-transaction [tx db/db]
    ;; first remove bringer
    (db/reset-bringer-for-day tx {:day date})
    ;; now remove all attendees
    (db/cancel-event tx {:day date})))
