(ns breakfastbot.refresh-names
  (:require [breakfastbot.db :as db]))

(defn- is-success? [reply]
  (= "success"  (:result reply)))

(defn parse-reply
  [reply]
  (if (is-success? reply)
    (->> reply
         :members
         (map (fn [m] [(:email m) (:full_name m)]))
         (into {}))))

(defn difference-zulip-and-db
  "Finds members to update from currently active members in database and reply
  from zulip members query"
  [db-members zulip-members]
  (for [{:keys [email fullname]} db-members
        :let                     [new-name (get zulip-members email)]
        :when                    (and  (contains? zulip-members email)
                                       (not= fullname new-name))]
    [email new-name]))

(defn update-db-from-diff
  "Applies diffs as computed by `difference-zulip-and-db to db-spec"
  [db-spec diffs]
  (for [[email new-name] diffs]
    (db/update-member-fullname db-spec {:email email :fullname new-name})))

(defn refresh-names [db-spec zulip-reply]
  (some->> zulip-reply
           parse-reply
           (difference-zulip-and-db (db/get-members db-spec))
           (update-db-from-diff db-spec)))
