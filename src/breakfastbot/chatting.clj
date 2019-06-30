(ns breakfastbot.chatting
  (:require [breakfastbot.config :refer [config]]
            [clojure-zulip.core :as zulip]
            [clojure.core.async :as a]
            [clojure.tools.logging :refer [info debug trace error]]
            [java-time :as jt]
            [mount.core :refer [defstate]]))

(defstate zulip-conn
  :start (zulip/connection (:zulip config)))

(defstate zulip-event-channels
  :start (zulip/event-queue zulip-conn)
  :stop (a/>!! (second zulip-event-channels) :stop))

(defn transmit-event
  "Passes event content and author to receiver and forwards output to sender in
  Zulip, either in public or private depending on where the message originated."
  [event receiver]
  (let [{stream       :display_recipient
         message-type :type
         sender       :sender_email
         :keys        [:subject :content]} (:message event)]
    ;; ignore messages sent by bot itself
    (when (and (some? content) (not= (-> config :zulip :username) sender))
      (trace "New message: \"" content "\" by" sender)
      (when-let [reply (receiver sender content)]
        ;; replies will always have a direct reply, send it first
        (let [direct-reply (:direct-reply reply)]
          ;; Reply to private message in private
          (if (= message-type "private")
            (zulip/send-private-message zulip-conn sender direct-reply)
            ;; otherwise post to stream
            (zulip/send-stream-message zulip-conn stream subject direct-reply)))
        (when-let [note (:notification reply)]
          (zulip/send-private-message zulip-conn (:who note) (:message note)))))))

(defn date->subject [date] (str "Breakfast " (jt/format "d.M.yyyy" date)))

(defn add-sync-handler [async-src sync-receiver]
  (let [kill-channel (a/chan)]
    (a/go-loop []
      (let [[msg channel] (a/alts! [kill-channel async-src] :priority true)]
        (cond (= channel kill-channel) (info "Async handler closing")
              :else                    (do (if-not (instance? Exception msg) (sync-receiver msg)
                                                   (error "Ignoring exception, continuing"))
                                           (recur)))))
    kill-channel))
