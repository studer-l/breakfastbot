(ns breakfastbot.handlers.add-member
  (:require [breakfastbot.chatting :refer [zulip-conn]]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.handlers.common :refer [answers]]
            [clojure-zulip.core :as zulip]
            [clojure.tools.logging :refer [debug]]))

(defn parse-add-member
  [_ message]
  (if (re-matches #"^add \S+\@\S+\.\S+$" message)
    (subs message 4)))

(defn- get-zulip-user-by-email [email]
  (debug "looking user matching email" email)
  (let [reply (zulip/sync* (zulip/members zulip-conn))]
    (when (= (:result reply) "success")
      (debug "Received user list from server, scanning for matching email..")
      (->> reply :members
           (filter #(= email (:email %)))
           first
           :full_name))))

(defn add-member-action
  [email]
  (let [fullname (get-zulip-user-by-email email)]
    (if fullname
      (do (db-ops/add-new-team-member email fullname)
          ((:welcome answers) fullname))
      (throw (ex-info (str "Could not find any user with email " email)
                      {:public true})))))

(def add-member-handler {:matcher parse-add-member
                         :action add-member-action
                         :help "\"@**breakfastbot** add email\" -- Adds new team member"})
