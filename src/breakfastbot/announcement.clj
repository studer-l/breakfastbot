(ns breakfastbot.announcement
  (:require [java-time :as jt]
            [breakfastbot.chatting :as chatting :refer [zulip-conn]]
            [breakfastbot.db-ops :as db-ops]
            [clojure.tools.logging :refer [debug]]
            [breakfastbot.db :as db]
            [clojure-zulip.core :as zulip]
            [breakfastbot.markdown :as md]))

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

(defn- time-to-announce?
  "Announce on ideally on friday, at 15:00 or any time later on the weekend"
  [datetime]
  (or (and (jt/friday? datetime)
           (<= 15 (jt/as datetime :clock-hour-of-day)))
      (any? ((juxt jt/saturday? jt/sunday?) datetime))))

(defn announce-breakfast
  "If not yet done so, announce breakfast on `date`"
  [datetime]
  (when-not (and (time-to-announce? datetime)
                 (:exists (db/have-bringer-for-day db/db {:day datetime})))
    (debug "Going to announcing breakfast...")
    (let [attendee-data (db-ops/prepare-breakfast db/db datetime)]
      (debug "Scheduled breakfast:" attendee-data)
      (zulip/send-stream-message zulip-conn "Monday Breakfast"
                                 (chatting/date->subject datetime)
                                 (announce-breakfast-message attendee-data))
      (debug "Breakfast announced!"))))
