(ns breakfastbot.handlers.override-test
  (:require [breakfastbot.handlers.override :as sut]
            [clojure.test :as t]
            [java-time :as jt]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.db :as db]
            [mount.core :as mount]
            [breakfastbot.db-test :refer [prepare-mock-db unpopular-date mock-emails]]))

(mount/start #'db/db)

;; parser already well tested by sign-off; this is just to ensure it matches `override me`
(t/deftest override-parser
  (t/testing "matches most simple string"
    (let [result (sut/parse-override  "person@company.com" "override me")]
      (t/is (= "person@company.com" (:who result)))
      (t/is (jt/local-date? (:when result))))))

(t/deftest override-bringer-action
  (t/testing "can override on known date"
    (prepare-mock-db)
    (t/is (= (:ack answers)
             (sut/override-bringer (-> mock-emails (nth 2) :email)
                                   unpopular-date))))
  (t/testing "refuses sing-on for dates where there is no breakfast"
    (t/is (= (:error-no-event answers)
             (sut/override-bringer "test@company.com" (jt/local-date))))))
