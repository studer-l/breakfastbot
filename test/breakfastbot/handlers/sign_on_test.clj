(ns breakfastbot.handlers.sign-on-test
  (:require [breakfastbot.handlers.sign-on :as sut]
            [java-time :as jt]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.db :as db]
            [mount.core :as mount]
            [breakfastbot.db-test :refer [prepare-mock-db unpopular-date mock-emails]]
            [clojure.test :as t]))

;; parser already well tested by sign-off

(mount/start #'db/db)

(t/deftest sign-on-action
  (t/testing "can sign on on known date"
    (prepare-mock-db)
    (t/is (= (:ok-happy answers)
             (sut/sign-on (-> mock-emails first :email) unpopular-date))))
  (t/testing "refuses sing-on for dates where there is no breakfast"
    (t/is (= (:error-no-event answers)
             (sut/sign-on "test@company.com" (jt/local-date))))))
