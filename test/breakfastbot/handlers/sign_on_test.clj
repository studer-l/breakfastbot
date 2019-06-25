(ns breakfastbot.handlers.sign-on-test
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-test :refer [prepare-mock-db unpopular-date mock-emails]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.handlers.sign-on :as sut]
            [clojure.test :as t]
            [java-time :as jt]
            [mount.core :as mount]))

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
    (t/is (= {:direct-reply (:ok-happy answers)
              :update       true}
             (sut/sign-on (-> mock-emails first :email) unpopular-date))))
  (t/testing "refuses sing-on for dates where there is no breakfast"
    (t/is (= {:direct-reply (:error-no-event answers)}
             (sut/sign-on "test@company.com" (jt/local-date))))))
