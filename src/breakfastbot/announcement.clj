(ns breakfastbot.announcement
  (:require [breakfastbot.chatting :as chatting :refer [zulip-conn]]
            [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.markdown :as md]
            [breakfastbot.date-utils :refer [next-monday]]
            [clojure-zulip.core :as zulip]
            [clojure.tools.logging :refer [debug info]]
            [java-time :as jt]))

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
  (str "🤖📣 BREAKFAST SCHEDULED 🤖📣\n\n"
       "Attendees:\n"
       (attendee-bullet-list attendee-data)
       "\nTotal attendees: " (-> attendee-data :attendees count Integer.)
       "\n\nResponsible for bringing Breakfast: "
       (md/mention (:fullname (:bringer attendee-data)))))

(defn time-to-announce?
  "Announce on ideally on friday, at 12:00 or any time later on the weekend"
  [datetime]
  (or (and (jt/friday? datetime)
           (<= 12 (jt/as datetime :clock-hour-of-day)))
      (some true? ((juxt jt/saturday? jt/sunday?) datetime))))

(defn- bringer-decided-on [date]
  (:exists (db/have-bringer-for-day db/db {:day date})))

(defn announce-breakfast
  "If not yet done so, announce next breakfast assuming the current time is `now-datetime`"
  [now-datetime]
  (let [next-event-date (next-monday now-datetime)]
    (when (and (time-to-announce? now-datetime)
               (not  (bringer-decided-on next-event-date)))
      (debug "Going to announcing breakfast if there is one")
      (when-let [attendee-data (db-ops/prepare-breakfast db/db next-event-date)]
        (debug "Scheduled breakfast:" attendee-data)
        (zulip/send-stream-message zulip-conn "Monday Breakfast"
                                   (chatting/date->subject next-event-date)
                                   (announce-breakfast-message attendee-data))
        (debug "Breakfast announced!")))))
