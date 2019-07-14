(ns breakfastbot.announcement
  (:require [breakfastbot.chatting :as chatting :refer [zulip-conn]]
            [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.refresh-names :refer [refresh-names]]
            [breakfastbot.markdown :as md]
            [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.config :refer [config]]
            [clojure-zulip.core :as zulip]
            [clojure.tools.logging :refer [debug info]]
            [java-time :as jt]
            [clojure.core.async :as a]))

(defn- attendee-bullet-list
  [prepare-result]
  (->> prepare-result
       :attendees
       (map :fullname)
       (map md/mention)
       (md/bullet-list)))

(defn announce-breakfast-message
  "Create breakfast announcement from attendee + bringer list"
  [attendee-data]
  (if (nil? attendee-data) "🤖📣 BREAKFAST CANCELED 🤖📣\n\n"
      (str "🤖📣 BREAKFAST SCHEDULED 🤖📣\n\n"
           "Attendees:\n"
           (attendee-bullet-list attendee-data)
           "\nTotal attendees: " (-> attendee-data :attendees count Integer.)
           "\n\nResponsible for bringing Breakfast: "
           (md/mention (:fullname (:bringer attendee-data))))))

(defn time-to-announce?
  "Announce on ideally on friday, at 12:00 or any time later on the weekend"
  [datetime]
  (or (and (jt/friday? datetime)
           (<= 12 (jt/as datetime :hour-of-day)))
      (some true? ((juxt jt/saturday? jt/sunday?) datetime))))

(defn- bringer-decided-on [date]
  (:exists (db/have-bringer-for-day db/db {:day date})))

(defn schedule-breakfast
  "If it is time, prepare next breakfast assuming the current time is `now-datetime`"
  [now-datetime]
  (let [next-event-date (next-monday now-datetime)]
    (when (and (time-to-announce? now-datetime)
               (db/any-attendance-on-date db/db {:day next-event-date})
               (not (bringer-decided-on next-event-date)))
      ;; also refresh names just to be sure
      (refresh-names db/db (zulip/sync* (zulip/members zulip-conn)))
      (debug "Appropriate time to announce breakfast and have not done it yet")
      (when-let [attendee-data (db-ops/prepare-breakfast db/db next-event-date)]
        (debug "Scheduled breakfast:" attendee-data)
        [next-event-date attendee-data]))))

(defn announce-breakfast-in-zulip
  "If not yet done so, announce next breakfast assuming the current time is
  `now-datetime`"
  [now-datetime]
  (when-let [[next-event-date attendee-data] (schedule-breakfast now-datetime)]
    (let [ch (zulip/send-stream-message zulip-conn
                                        (-> config :bot :channel)
                                        (chatting/date->subject next-event-date)
                                        (announce-breakfast-message attendee-data))]
      (db/insert-announce-msg-id db/db {:day next-event-date
                                        :id (-> ch a/<!! :id)}))
    (debug "Breakfast announced!")))

(defn update-current-announcement
  ([db] (update-current-announcement db (next-monday)))
  ([db date]
   (if-let [msg-id (-> db (db/get-announce-msg-id {:day date}) :id)]
     (let [attendee-data (db-ops/prepare-breakfast db date)]
       (zulip/update-message zulip-conn msg-id
                             (announce-breakfast-message attendee-data))))))
