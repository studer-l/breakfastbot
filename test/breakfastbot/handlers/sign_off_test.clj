(ns breakfastbot.handlers.sign-off-test
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.db :as db]
            [breakfastbot.db-test :refer [prepare-mock-db next-date]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.handlers.sign-off :as sut]
            [clojure.test :as t]
            [java-time :as jt]
            [mount.core :as mount]))

(mount/start #'db/db)

(def today-str (jt/format "d.M.yyy" (jt/local-date)))
(def tomorrow (jt/plus (jt/local-date) (jt/days 1)))
(def tomorrow-str (jt/format "d.M.yyy" tomorrow))

(t/deftest parse-sign-off
  (t/testing "valid messages"
    (t/is (= {:who "person@company.com" :when (jt/local-date)}
             (sut/parse-sign-off "person@company.com"
                                 (str "cannot " today-str))))
    (t/is (= {:who "person@company.com" :when tomorrow}
             (sut/parse-sign-off "person@company.com"
                                 (str "cannot me " tomorrow-str))))
    (t/is (= {:who "somebody@else.com" :when (jt/local-date)}
             (sut/parse-sign-off "another.person@company.com"
                                 (str "cannot somebody@else.com " today-str))))
    (t/is (= {:who "another@one.com" :when (next-monday)}
             (sut/parse-sign-off "person@company.com"
                                 "cannot another@one.com")))
    (t/is (= {:who "person@company.com" :when (next-monday)}
             (sut/parse-sign-off "person@company.com"
                                 "cannot me")))
    (t/is (= {:who "person@company.com" :when (next-monday)}
             (sut/parse-sign-off "person@company.com"
                                 "cannot"))))
  (t/testing "invalid messages"
    (t/is (thrown? Exception (sut/parse-sign-off "bad.date.order@company.org"
                                                 "cannot me 3.28.2018"))))
  (t/testing "reject far off dates"
    (t/is (thrown? Exception (sut/parse-sign-off "a@b.c"
                                                 (jt/local-date 2300 1 1))))
    (t/is (thrown? Exception (sut/parse-sign-off "date.in.far.future@company.org"
                                                 "cannot me 1.1.2280"))))
  (t/testing "rejects dates in the past"
    (t/is (thrown? Exception (sut/parse-sign-off "date.in.past@company.org"
                                                 "cannot me 1.1.1980")))))

(def date next-date)

;; testing the handler is superbly complicated
(t/deftest test-sign-off-action
  (t/testing "can sign-off prior to commitment"
    (t/is (.startsWith 
             (:direct-reply (do (prepare-mock-db)
                 ;; at this stage no bringer is selected for this date yet
                 (sut/sign-off "marissa.mucci@company.com" date date))) (:ok-unhappy answers)))
    (t/is (= true
             (:update (do (prepare-mock-db)
                 ;; at this stage no bringer is selected for this date yet
                 (sut/sign-off "marissa.mucci@company.com" date date)))))
  (t/testing "cannot sign-off twice"
    (t/is (= {:direct-reply (:error-signed-off answers)}
             (sut/sign-off "marissa.mucci@company.com" date date))))
  (t/testing "cannot sign-off from non-existant event"
    (t/is (= {:direct-reply (:error-no-event answers)}
             (sut/sign-off "marissa.mucci@company.com"
                           (jt/plus date (jt/days 1))
                           date))))
  (t/testing "conflicts are resolved"
    (db/set-bringer-by-email db/db
                             {:day date :email "catherina.carollo@company.com"})
    (t/is (= {:direct-reply ((:change-bringer answers) "miles.mcinnis@company.com")
              :update       true
              :notification {:who     "miles.mcinnis@company.com"
                             :message (:new-bringer answers)}}
             (sut/sign-off "catherina.carollo@company.com" date date))))
  (t/testing "when the last person signs off, breakfast is canceled"
    (t/is (= {:direct-reply (:ok-unhappy answers)
              :update       true}
             (sut/sign-off "stan.sandiford@company.com" date date)))
    (t/is (= {:direct-reply ((:cancel answers) date)
              :update       true}
             (sut/sign-off "miles.mcinnis@company.com" date date))))
  (t/testing "reports error when no member is found"
    (t/is (= {:direct-reply (:error-no-member answers)}
             (sut/sign-off "imaginary.person@company.com" date date)))))
