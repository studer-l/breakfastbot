(ns breakfastbot.handlers.sign-on-test
  (:require [breakfastbot.handlers.sign-on :as sut]
            [java-time :as jt]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.db :as db]
            [mount.core :as mount]
            [breakfastbot.db-test :refer [prepare-mock-db unpopular-date mock-emails]]
            [clojure.test :as t]))

(mount/start #'db/db)

;; parser already well tested by sign-off; this is just to ensure it matches `can me`
(t/deftest sign-on-parser
  (t/testing "matches most simple string"
    (let [result (sut/parse-sign-on  "person@company.com" "can me")]
      (t/is (= "person@company.com" (:who result)))
      (t/is (jt/local-date? (:when result))))))

(t/deftest sign-on-action
  (t/testing "can sign on on known date"
    (prepare-mock-db)
    (t/is (= (:ok-happy answers)
             (sut/sign-on (-> mock-emails first :email) unpopular-date))))
  (t/testing "refuses sing-on for dates where there is no breakfast"
    (t/is (= (:error-no-event answers)
             (sut/sign-on "test@company.com" (jt/local-date))))))
