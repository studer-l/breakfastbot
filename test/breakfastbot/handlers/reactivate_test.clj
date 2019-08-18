(ns breakfastbot.handlers.reactivate-test
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.db-test :refer [prepare-mock-db popular-date next-date
                                          mock-users]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.handlers.reactivate :as sut]
            [clojure.test :as t]
            [java-time :as jt]
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
             (sut/reactivate-member member-email))))
  (t/testing "re-activating also primes for upcoming events"
    (prepare-mock-db)
    ;; create de-activated member not primed for any breakfast
    (db/insert-member db/db {:email "foo@quux.bar" :fullname "Foo Quux"})
    (db/change-member-active db/db {:email "foo@quux.bar" :active false})
    (sut/reactivate-member "foo@quux.bar" popular-date (jt/plus next-date (jt/days 10)))
    (t/is (= (into #{} (conj mock-users {:email "foo@quux.bar" :fullname "Foo Quux"}))
             (into #{} (db/get-all-attendees db/db {:day next-date}))))))
