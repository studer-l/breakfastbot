(ns breakfastbot.handlers.who-brings
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.db :as db]
            [java-time :as jt]))

(defn who-brings [date]
  (if-let [bringer (db/get-bringer-on db/db {:day date})]
    {:direct-reply (str "Official Bringer of Breakfast on "
                        (jt/format "d.M" date) " : **@"
                        (:fullname bringer) "**")}
    (throw (ex-info "Don't know yet" {:public true}))))

(def who-handler {:matcher (fn [author message] (= message "who"))
                  :action (fn [_] (who-brings (next-monday)))
                  :help (str "\"@**breakfastbot** who\" -- "
                             "Ask who will bring breakfast next")})
