(ns breakfastbot.db-test
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [clojure.test :as t]
            [java-time :as jt]
            [mount.core :as mount]))

(mount/start #'db/db)

;; setup test db
(defn reset-db! [db]
  (db/drop-bringer-table db)
  (db/drop-attendances-table db)
  (db/drop-members-table db)
  (db/drop-app-state-table db)
  (db/create-members-table db)
  (db/create-attendances-table db)
  (db/create-app-state-table db)
  (db/create-bringer-table db)
  (db/insert-initial-app-state db))

;; add some users
(def mock-emails [{:email "stan.sandiford@company.com"}
                  {:email "catherina.carollo@company.com"}
                  {:email "miles.mcinnis@company.com"}
                  {:email "marissa.mucci@company.com"}])

(def mock-names [{:fullname "Stan Sandiford"}
                 {:fullname "Cahterina Carollo"}
                 {:fullname "Miles McInnis"}
                 {:fullname "Marisssa Mucci"}])

(def mock-users
  (map #(apply merge %)
       (map vector mock-emails mock-names)))

(t/deftest insert-member
  (t/testing "can insert mock member data, ids are auto incremented"
    (t/is (= (do (reset-db! db/db)
                 (for [user mock-users]
                   (db/insert-member db/db user)))
             (for [id (range 1 5)] {:id id})))))

(def popular-date (jt/local-date 2019 1 1))
(def not-so-popular-date (jt/local-date 2019 1 2))
(def unpopular-date (jt/local-date 2019 1 3))
(def next-date (jt/local-date 2019 1 4))

(defn email-of [i] (-> mock-emails (nth i) :email))

(def mock-attendance
  ;; Everyone attended breakfast on 2019 / 01 / 01
  [{:day popular-date :email (email-of 0)}
   {:day popular-date :email (email-of 1)}
   {:day popular-date :email (email-of 2)}
   {:day popular-date :email (email-of 3)}
   ;; on 2019 / 01 / 02 only two showed up
   {:day not-so-popular-date :email (email-of 1)}
   {:day not-so-popular-date :email (email-of 2)}
   ;; on 2019 / 01 / 03 only one person
   {:day unpopular-date :email (email-of 2)}
   ;; on 2019 / 01 / 04 lots of people show up again
   {:day next-date :email (email-of 1)}
   {:day next-date :email (email-of 2)}
   {:day next-date :email (email-of 3)}])

(def mock-bringings
  [{:day popular-date :email (email-of 0)}
   {:day not-so-popular-date :email (email-of 2)}
   {:day unpopular-date :email (email-of 2)}])

(t/deftest insert-attendances
  (t/testing "can insert mock attendance data"
    (t/is (every? #(= 1 %)
                  (do (reset-db! db/db)
                      (doall (for [user mock-users]
                               (db/insert-member db/db user)))
                      (doall (for [attendance mock-attendance]
                               (db/insert-attendance-by-email db/db attendance))))))
    (t/testing "can get back all attendees on a given date by email"
      (t/is (= #{(nth mock-users 1)
                 (nth mock-users 2)}
               (set (db/get-all-attendees db/db {:day not-so-popular-date})))))))

(defn prepare-mock-db []
  (reset-db! db/db)
  (doall (for [user mock-users]
           (db/insert-member db/db user)))
  (doall (for [attendance mock-attendance]
           (db/insert-attendance-by-email db/db attendance)))
  (doall (for [bringing mock-bringings]
           (db/set-bringer-by-email db/db bringing)))
  ;; also sign up everyone for the next breakfast
  (db-ops/prime-breakfast next-date))

;; By the time of the next-date the counts for attendance since brining are as
;; follows:
;; Catherina - Attended `popular-date`,`not-so-popular-date` -> 2
;; Miles - brought breakfast on `unpopular-date` -> 0
;; Marissa: attended `popular-date` -> 1
;; Now postgres numbers the IDs starting from 1, so the expected counts are:
(def expected-counts-at-next-date #{{:id 2 :count 2}
                                    {:id 3 :count 0}
                                    {:id 4 :count 1}})

(t/deftest prepare-breakfast
  (t/testing "can insert bringings"
    (t/is (= [1 1 1]
             (do (reset-db! db/db)
                 (doall (for [user mock-users]
                          (db/insert-member db/db user)))
                 (doall (for [attendance mock-attendance]
                          (db/insert-attendance-by-email db/db attendance)))
                 (doall (for [bringing mock-bringings]
                          (db/set-bringer-by-email db/db bringing))))))
    (t/testing "can count number of attendances since last bringing"
      (t/is (= expected-counts-at-next-date
               (set
                (db/get-attendance-counts-since-bringing
                 db/db {:day next-date})))))
    (t/testing "chooses the person who attended the most events without bringing as next"
      (t/is (= (nth mock-users 1)
               (db-ops/choose-bringer db/db next-date))))
    (t/testing "updates the bringer table"
      (t/is (= (nth mock-users 1)
               (db/get-bringer-on db/db {:day next-date}))))))

(t/deftest prime-db
  (t/testing "can prime single date"
    (t/is (= (set mock-users)
             (set
              (do (reset-db! db/db)
                  ;; insert some mock users
                  (doall (for [user mock-users] (db/insert-member db/db user)))
                  ;; sign them all up for breakfast today
                  (db-ops/prime-breakfast (jt/local-date))
                  ;; retrieve result
                  (db/get-all-attendees db/db {:day (jt/local-date)})))))))

(t/deftest add-new-member
  (t/testing "can add new member"
    (t/is (some #(= % {:fullname "nat"
                       :email "natalie.newcomer@company.com"})
                (do (reset-db! db/db)
                    ;; insert mock team
                    (doall (for [user mock-users] (db/insert-member db/db user)))
                    ;; sign them up for ALL THE BREAKFASTS
                    (db-ops/prime-attendance db/db)
                    ;; When we add a new member
                    (db-ops/add-new-team-member
                     "natalie.newcomer@company.com" "nat")
                    ;; Then they are added to the existing events
                    (db/get-all-attendees db/db {:day (next-monday)}))))))
