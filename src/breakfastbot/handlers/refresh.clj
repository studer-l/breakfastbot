(ns breakfastbot.handlers.refresh
  (:require [breakfastbot.chatting :refer [zulip-conn]]
            [breakfastbot.db :as db]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.refresh-names :as impl]
            [clojure-zulip.core :as zulip]))

(defn refresh-names! []
  (impl/refresh-names db/db (zulip/sync* (zulip/members zulip-conn)))
  {:direct-reply (:ack answers)})

(def refresh-handler {:matcher (fn [_ message] (= message "refresh"))
                      :action  refresh-names!
                      :help    "Secret admin action: Refreshes names"})
