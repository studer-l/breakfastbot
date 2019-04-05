(ns breakfastbot.core
  (:require [breakfastbot.chatting :refer [transmit-event zulip-event-channels
                                           add-sync-handler]]
            [breakfastbot.actions :refer [handlers dispatch-handlers]]
            [breakfastbot.config :refer [config]]
            [breakfastbot.db-ops :refer [prime-attendance]]
            [breakfastbot.db :refer [db]]
            [breakfastbot.chores :refer [attendance-prime-task
                                         announce-breakfast-task]]
            [mount.core :as mount :refer [defstate]]
            [migratus.core :as migratus]
            [mount-up.core :as mu]
            [clojure.tools.logging :refer [info]]
            [clojure.core.async :as a])
  (:gen-class))


(defstate chat-bot
  ;; attach handlers to event stream
  :start (add-sync-handler
          (first zulip-event-channels)
          (fn [event]
            (transmit-event event
                            (fn [author content]
                              (dispatch-handlers handlers author content)))))
  :stop (a/>!! chat-bot :stop))

(defn as-migratus-config [config]
  {:store :database
   :init-script "init.sql"
   :migration-table-name "migratus"
   :db (:db config)})

(defn -main
  [& args]
  (mu/on-upndown :info mu/log :before)
  ;; perform migrations first
  (mount/start #'config)
  (let [migratus-config (as-migratus-config config)]
    (migratus/init migratus-config)
    (if-let [pending (migratus/pending-list migratus-config)]
      (do (info "Migrations pending: " pending)
          (migratus/migrate migratus-config))
      (info "Database up to date")))
  (mount/start)
  ;; ensure we are primed for next 30 days now rather than waiting for background task
  (prime-attendance db))
