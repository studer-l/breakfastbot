(ns breakfastbot.handlers.common
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.markdown :as md]
            [clojure.tools.logging :refer [info debug]]
            [java-time :as jt]))

(def answers {:ok-unhappy "Alright 🙄"
              :ok-happy "Great!"
              :ack "🤖 ACKNOWLEDGED 🤖"
              :error-already-signed-off "ERROR: You already signed off! 😤"
              :error-no-event "ERROR: No event scheduled for this date 👎"
              :change-responsible
              (fn [fullname]
                (str "OK 🙄 New responsible for bringing breakfast is "
                     (md/mention fullname)))
              :cancel "BREAKFAST CANCELED!"
              :welcome (fn [email] (str "🎉🎈 Welcome " (md/mention email) "!! 🎉🎈"))})

(defn try-parse-date
  [when]
  (try (jt/local-date "d.M.yyyy" when)
       (catch Exception ex
         (info "Date parsing failed for input:" when)
         (throw (ex-info
                 (str "Could not parse as valid breakfast date: \"" when
                      "\", example format: \"23.4.2018\", must be a monday")
                 {:public true})))))

(defn validate-date [date]
  (cond (< 5 (jt/time-between (jt/local-date) date :months))
        (throw
         (ex-info (str "The date " (jt/format "d.M.yyyy" date)
                       " is too far in the future!") {:public true}))
        (neg? (jt/time-between (jt/local-date) date :days))
        (throw
         (ex-info (str "The date " (jt/format "d.M.yyyy" date)
                       " is in the past!") {:public true}))
        :else date))

(defn person-date-matcher
  [regex author message]
  (let [matcher (re-matcher regex message)]
    (if (re-find matcher)
      (let [[who when] (rest (re-groups matcher))]
        {:who (if (or (nil? who) (= who "me")) author who)
         :when (if-not when (next-monday)
                       (let [date (try-parse-date when)]
                         (validate-date date)))}))))
