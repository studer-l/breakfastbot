(ns breakfastbot.handlers.refresh
  (:require [breakfastbot.chatting :refer [zulip-conn]]
            [breakfastbot.db :as db]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.refresh-names :refer [refresh-names]]
            [clojure-zulip.core :as zulip]))

(defn refresh-action [_]
  (refresh-names db/db (zulip/sync* (zulip/members zulip-conn)))
  {:direct-reply (:ack answers)
   :update       true})

(def refresh-handler {:matcher (fn [_ message] (= message "refresh"))
                      :action  refresh-action
                      :help    "Secret admin action: Refreshes names"})
