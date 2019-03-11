(ns breakfastbot.handlers.help
  (:require [clojure.string :as s]))

(defn handlers->help-handler
  [other-handlers]
  {:matcher (fn [_ message] (= message "help"))
   :action (fn [_] (s/join "\n" (map :help other-handlers)))
   :help "No help for help"})
