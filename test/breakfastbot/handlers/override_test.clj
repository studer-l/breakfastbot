(ns breakfastbot.handlers.override-test
  (:require [breakfastbot.db :as db]
            [breakfastbot.db-test :refer [prepare-mock-db unpopular-date
                                          next-date mock-emails]]
            [breakfastbot.handlers.common :refer [answers]]
            [breakfastbot.handlers.override :as sut]
            [clojure.test :as t]
            [java-time :as jt]
            [mount.core :as mount]))

(mount/start #'db/db)

;; parser already well tested by sign-off; this is just to ensure it matches `override me`
(t/deftest override-parser
  (t/testing "matches most simple string"
    (let [result (sut/parse-override  "person@company.com" "override person@company.com")]
      (t/is (= ["person@company.com"] (:who result)))
      (t/is (jt/local-date? (:when result)))))
  (t/testing "matches multiple"
    (let [result (sut/parse-override  "person@company.com" "override a@b.c d@e.f")]
      (t/is (= ["a@b.c" "d@e.f"] (:who result)))))
  (t/testing "handles help"
    (t/is (nil? (sut/parse-override "person@company.com" "help")))))

(t/deftest override-bringer-action
  (t/testing "can override on known date"
    (prepare-mock-db)
    (t/is (= {:direct-reply (:ack answers) :update true}
             (sut/override-bringer [(-> mock-emails (nth 2) :email)]
                                   unpopular-date))))
  (t/testing "refuses sing-on for dates where there is no breakfast"
    (t/is (= {:direct-reply (:error-no-event answers)}
             (sut/override-bringer "test@company.com" (jt/local-date)))))
  (t/testing "can set bringer on date where no bringer is set yet"
    (t/is (= {:direct-reply (:ack answers) :update true}
             (sut/override-bringer [(-> mock-emails first :email)]
                                   next-date)))))

(t/deftest override-many-bringers
  (prepare-mock-db)
  (t/testing "can override multiple bringers"
    (let [emails ["catherina.carollo@company.com" "marissa.mucci@company.com"]]
      (t/is (= {:direct-reply (:ack answers) :update true}
               (sut/override-bringer emails next-date)))
      (t/is (= (list {:email "catherina.carollo@company.com", :fullname "Cahterina Carollo"}
                     {:email "marissa.mucci@company.com", :fullname "Marisssa Mucci"})
               (db/get-bringers-on db/db {:day next-date}))))))
