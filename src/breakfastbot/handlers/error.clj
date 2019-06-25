(ns breakfastbot.handlers.error)

(def bb-error-handler
  {:matcher (fn [_ _] true)
   :action  {:direct-reply (str "Could not understand 🤖\n"
                                "Try `@**Breakfastbot** help` to see a "
                                "list of commands")}
   :help    "Can't help you any further than that"})
