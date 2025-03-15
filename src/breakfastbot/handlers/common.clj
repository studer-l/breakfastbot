(ns breakfastbot.handlers.common
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.markdown :as md]
            [clojure.tools.logging :refer [info debug]]
            [breakfastbot.handlers.eliza :refer [get-eliza-reply]]
            [java-time :as jt]
            [clojure.string :as str]))

(def welcome-help
  (str "🤖 WELCOME TO DISTRAN HUMAN 🤖\n"
       "* I organize breakfast for every Monday at 9:00\n"
       "* Attendance is limited to puny humans 🙄\n"
       "* Every week I choose two members of the team to bring breakfast\n"
       "* Let me know if you cannot make it\n"
       "* To learn more, send me the message (in private or in the breakfast "
       "channel) `@**Breakfast Bot** help`, this is best achieved by typing"
       " `@Br` and then hitting the `<TAB>` key to complete my name.\n\n"
       "Study the code on [Github](https://github.com/studer-l/breakfastbot)\n"
       "Want to talk to a human? Ask @**Lukas Studer** for help."))

(def reactivation-msg
  (str "🤖 YOU HAVE BEEN REACTIVATED AND"
       " ARE EXPECTED TO ATTEND BREAKFASTS AGAIN 🤖"))

(defn who-brings-answer
  [names date]
  (str "Official Bringer(s) of Breakfast on " (jt/format "d.M" date)
       ": " (str/join " and " (map md/mention names))))

(def answers {:ok-unhappy       "Alright 🙄"
              :ok-happy         "Great!"
              :eliza-reply      get-eliza-reply
              :ack              "🤖 ACKNOWLEDGED 🤖"
              :error-signed-off "ERROR: Already signed off! 😤\nDo you often have your head in the clouds?"
              :error-no-event   "ERROR: No event scheduled for this date.\nDoes this make you feel sad?"
              :error-no-member  "ERROR: Noone by this email is registered! 💣\nIs there anything else I can do for you?"
              :error-active     "ERROR: Already marked as active!"
              :change-bringer   (fn [fullnames]
                                  (str "OK 🙄 Breakfast duty relegated to: "
                                       (str/join " and " (map md/mention fullnames))))
              :cancel           (fn [when]
                                  (str "BREAKFAST ON " (jt/format when)
                                       " CANCELED!"))
              :welcome          (fn [email]
                                  (str "🎉🎈 Welcome " (md/mention email)
                                       "!! 🎉🎈"))
              :reactivate       reactivation-msg
              :welcome-help     welcome-help
              :new-bringer      "HUMAN! 🤖 You have been chosen to bring breakfast!"
              :who-brings       who-brings-answer})

(defn changed-bringer?
  "Checks whether result of `db-ops/safe-remove implies a new person is
  responsible for breakfast"
  [result]
  (and (seqable? result)
       (= (first result) :ok-new-responsible)))

(defn change-bringer-reply
  "Create appropriate (full) reply when changing bringer"
  [new-bringer]
  {:direct-reply ((:change-bringer answers) new-bringer)
   :notification {:who     new-bringer
                  :message (:new-bringer answers)}
   :update       true})

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
    (when (.matches matcher)
      (let [who (.group matcher "who")
            when (.group matcher "when")]
        (debug "parson-date-matcher, re-groups = " (re-groups matcher) ", who =" who ", when =" when)
        {:who (if (or (nil? who) (= who "me")) author who)
         :when (if-not when (next-monday)
                       (let [date (try-parse-date when)]
                         (validate-date date)))}))))
