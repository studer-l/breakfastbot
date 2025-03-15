(ns breakfastbot.handlers.deactivate-test
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [breakfastbot.db-test :refer [prepare-mock-db
                                          not-so-popular-date
                                          unpopular-date
                                          next-date mock-emails]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.handlers.deactivate :as sut]
            [clojure.test :as t]
            [mount.core :as mount]))

(mount/start #'db/db)

(t/deftest deactivate-parser
  (t/testing "matches string"
    (let [result (sut/parse-deactivate-member  "admin@company.com"
                                               "deactivate someone@company.com")]
      (t/is (= "someone@company.com" result)))))

(t/deftest deactivate-action
  (prepare-mock-db)
  (t/testing "error if member does not exist"
    (t/is (= {:direct-reply (:error-no-member answers)}
             (sut/deactivate-member next-date "unknown@nosuch.com"))))
  (let [email (-> mock-emails second :email)]
    (t/testing "initially is signed up"
      (t/is (some? (db-ops/attends-event? email not-so-popular-date))))
    (t/testing "succeeds with signed up member"
      (t/is (= {:direct-reply (:ok-unhappy answers)
                :update       true}
               (sut/deactivate-member not-so-popular-date email))))
    (t/testing "then is removed from all future events"
      (t/is (nil? (db-ops/attends-event? email not-so-popular-date)))
      (t/is (nil? (db-ops/attends-event? email unpopular-date)))
      (t/is (nil? (db-ops/attends-event? email next-date))))))

(t/deftest deactivate-action-cancel
  (prepare-mock-db)
  (let [email (-> mock-emails (nth 2) :email)]
    (t/testing "initially is signed up"
      (t/is (some? (db-ops/attends-event? email unpopular-date))))
    (t/testing "succeeds, canceling event"
      (t/is (= {:direct-reply ((:cancel answers) unpopular-date)
                :update       true}
               (sut/deactivate-member unpopular-date email))))))

(t/deftest deactivate-action-reassign-and-cancel
  (prepare-mock-db)
  ;; in fixture above bringer is already set for unpopular, un-do that
  (db/reset-bringer-for-day db/db {:day unpopular-date})
  (let [email (-> mock-emails (nth 2) :email)]
    (t/testing "succeeds, marking other person as bringer"
      (t/is (= {:direct-reply ((:change-bringer answers) [(-> mock-emails second :email)])
                :notification {:who (list "catherina.carollo@company.com")
                               :message (:new-bringer answers)}
                :update       true}
               (sut/deactivate-member not-so-popular-date email)))))
  (let [email (-> mock-emails second :email)]
    (t/testing "succeeds, canceling the breakfast"
      (t/is (= {:direct-reply ((:cancel answers) not-so-popular-date)
                :update       true}
               (sut/deactivate-member not-so-popular-date email))))))

(t/deftest deactivate-already-deactivated
  (prepare-mock-db)
  (t/testing "error if already deactivated"
    (let [email (-> mock-emails first :email)]
      (t/is some? (sut/deactivate-member next-date email))
      (t/is some? (sut/deactivate-member next-date email)))))
