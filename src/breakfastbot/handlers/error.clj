(ns breakfastbot.handlers.error)

(def bb-error-handler
  {:matcher (fn [_ message] message)
   :action  (fn [message] {:direct-reply ((:eliza-reply answers) message)})
   :help    "Can't help you any further than that"})
