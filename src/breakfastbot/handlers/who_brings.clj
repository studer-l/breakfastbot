(ns breakfastbot.handlers.who-brings
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.db :as db]))

(defn who-brings [date]
  (if-let [bringer (db/get-bringer-on db/db {:day date})]
    {:direct-reply ((:who-brings answers) (:fullname bringer) date)}
    (throw (ex-info "Don't know yet" {:public true}))))

(def who-handler {:matcher (fn [author message] (= message "who"))
                  :action (fn [_] (who-brings (next-monday)))
                  :help (str "\"@**breakfastbot** who\" -- "
                             "Ask who will bring breakfast next")})
