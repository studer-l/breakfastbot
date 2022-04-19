(ns breakfastbot.handlers.who-brings-test
  (:require [breakfastbot.handlers.who-brings :as sut]
            [clojure.test :as t]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.db-test :refer [prepare-mock-db unpopular-date
                                          random-date]]))

(t/deftest who-brings-action
  (prepare-mock-db)
  (t/testing "exception thrown if not known"
    (t/is (thrown? clojure.lang.ExceptionInfo (sut/who-brings random-date))))
  (t/testing "message formatted if known"
    (t/is (= {:direct-reply ((:who-brings answers)
                             ["Miles McInnis"] unpopular-date)}
             (sut/who-brings unpopular-date)))))
