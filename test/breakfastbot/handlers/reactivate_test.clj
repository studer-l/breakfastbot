(ns breakfastbot.handlers.reactivate-test
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.db-test :refer [prepare-mock-db
                                          not-so-popular-date
                                          unpopular-date
                                          next-date mock-emails]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.handlers.reactivate :as sut]
            [clojure.test :as t]
            [mount.core :as mount]))


(mount/start #'db/db)

(def member-email "stan.sandiford@company.com")

(t/deftest reactivate-parser
  (t/testing "matches string"
    (t/is (= member-email
             (sut/parse-reactivate-member "admin@company.com"
                                          (str "reactivate " member-email))))))

(t/deftest reactivate-action
  (prepare-mock-db)

  (t/testing "error if member does not exist"
    (t/is (= {:direct-reply (:error-no-member answers)}
             (sut/reactivate-member "unknown@nosuch.com"))))

  ;; deactivate member to test happy path
  (db/change-member-active db/db {:email member-email :active false})
  (t/testing "when re-activating a previously de-activated member"
    (t/is (= {:direct-reply (:ack answers)
              :notification {:who     member-email
                             :message (:reactivate answers)}
              :update       true}
             (sut/reactivate-member member-email)))
    (t/is (= true (db-ops/is-active? db/db member-email))))
  (t/testing "re-activating an already active member is an error"
    (t/is (= {:direct-reply (:error-active answers)}
             (sut/reactivate-member member-email)))))
