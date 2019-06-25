(ns breakfastbot.actions
  (:require [breakfastbot.handlers.add-member :refer [add-member-handler]]
            [breakfastbot.handlers.help :refer [handlers->help-handler]]
            [breakfastbot.handlers.override :refer [override-bringer-handler]]
            [breakfastbot.handlers.sign-off :refer [sign-off-handler]]
            [breakfastbot.handlers.sign-on :refer [sign-on-handler]]
            [breakfastbot.handlers.who-brings :refer [who-handler]]
            [breakfastbot.handlers.deactivate :refer [deactivate-handler]]
            [breakfastbot.handlers.error :refer [bb-error-handler]]
            [breakfastbot.announcement :refer [update-current-announcement]]
            [clojure.string :as s]
            [clojure.tools.logging :refer [info error fatal debug]]
            [breakfastbot.db :as db])
  (:import (org.postgresql.util PSQLException)))

;; Structure of Handler is a map with the keys :matcher :action  and :help
;;
;; :matcher
;; ========
;; Given the message author's email and the message string without the leading
;; call sequence ('@**breakfastbot**') as input, should return truthy if this
;; action should fire
;; To communicate with users about failure at the matching stage, throw an
;; ExceptionInfo with the `:public` set to truthy, the message will then be
;; replied to the user.
;;
;; :action
;; =======
;; If matcher above returned truthy, this function is called with the return
;; value of matcher.
;; It should return a string that is sent to the user as a reply
;;
;; :help
;; ======
;; Single line (formated) help string

;; all handlers except help-handler
(def basic-handlers [who-handler sign-off-handler add-member-handler
                     sign-on-handler override-bringer-handler deactivate-handler])
(def handlers (conj basic-handlers
                    (handlers->help-handler basic-handlers)
                    bb-error-handler))

(def help-handler
  (handlers->help-handler basic-handlers))

(defn try-handler
  [handler author content]
  (if-let [args ((:matcher handler) author content)]
    (do (info "Matched with hander" (type (:matcher handler)))
        ((:action handler) args))))

(defn- format-error [msg]
  (str "💥 🤖 🔥 ERORR: " msg))

;; matches a full command consisting of trigger sequence + action
(def trigger-re #"^@\*\*Breakfast Bot\*\*\W+.+")

;; only matches the initial sequence (missing `.+` at the end)
(def clean-re #"^@\*\*Breakfast Bot\*\*\W+")

(defn- strip-trigger-word [msg]
  (s/replace-first msg clean-re ""))

(defn- starts-with-trigger? [msg]
  (if msg (re-matches trigger-re msg)))

(defn dispatch-handlers [handlers author content]
  (if (starts-with-trigger? content)
    (let [cmd (strip-trigger-word content)]
      (try
        (debug "trying to match cmd:" cmd)
        (let [result (some (fn [handler] (try-handler handler author cmd))
                           handlers)]
          (if (:update result)
            (try (update-current-announcement db/db)
                 (catch org.postgresql.util.PSQLException ex
                   (error "Caught postgres exception:\n"
                          (-> ex Throwable->map :cause)
                          "\nwhen trying to update announcement message, this is"
                          "a benign error at test time"))))
          result)
        (catch clojure.lang.ExceptionInfo ex
          (error "Handler called by" author "with \"" content
                 "\" caught exception" ex "")
          (if (-> ex ex-data :public) {:direct-reply (format-error (.getMessage ex))}))
        (catch Exception ex
          (fatal "Unexpected error processing message by " author "content: \""
                 content "\" exception:" ex)
          (throw ex))))))
