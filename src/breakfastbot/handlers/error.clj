(ns breakfastbot.handlers.error
  (:require [breakfastbot.handlers.common :refer [answers]]))

(def bb-error-handler
  {:matcher (fn [_ message] message)
   :action  (fn [message] {:direct-reply ((:eliza-reply answers) message)})
   :help    "Can't help you any further than that"})
