(ns breakfastbot.handlers.who-brings
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.db :as db]
            [clojure.tools.logging :as log]))

(defn who-brings [date]
  (if-let [bringers (seq (db/get-bringers-on db/db {:day date}))]
    (do (log/debug "got bringers" bringers)
        {:direct-reply ((:who-brings answers) (map :fullname bringers) date)})
    (throw (ex-info "Don't know yet" {:public true}))))

(def who-handler {:matcher (fn [_ message] (= message "who"))
                  :action  (fn [_] (who-brings (next-monday)))
                  :help    (str "\"@**breakfastbot** who\" -- "
                                "Ask who will bring breakfast next")})
