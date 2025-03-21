(ns breakfastbot.db-test
  (:require [breakfastbot.date-utils :refer [next-monday]]
            [breakfastbot.db :as db]
            [breakfastbot.db-ops :as db-ops]
            [clojure.test :as t]
            [java-time :as jt]
            [mount.core :as mount]))

(mount/start #'db/db)

;; setup test db
(defn create-db! [db]
  (db/create-members-table db)
  (db/create-attendances-table db)
  (db/create-app-state-table db)
  (db/create-bringer-table db)
  (db/create-announce-msg-table db)
  (db/insert-initial-app-state db))

(defn drop-db! [db]
  (db/drop-bringer-table db)
  (db/drop-attendances-table db)
  (db/drop-members-table db)
  (db/drop-announce-msg-table db)
  (db/drop-app-state-table db))

(defn reset-db! [db]
  (drop-db! db)
  (create-db! db))

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
(def next-date (jt/local-date 2019 1 7))

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

(defn populate-db! []
  (doall (for [user mock-users]
           (db/insert-member db/db user)))
  (doall (for [attendance mock-attendance]
           (db/insert-attendance-by-email db/db attendance)))
  (doall (for [bringing mock-bringings]
           (db/set-bringer-by-email db/db bringing)))
  ;; also sign up everyone for the next breakfast
  (db-ops/prime-breakfast next-date))

(defn prepare-mock-db []
  (reset-db! db/db)
  (populate-db!))

;; By the time of the next-date the counts for attendance since brining are as
;; follows:
;; Catherina - Attended `popular-date`,`not-so-popular-date` -> 2
;; Miles - brought breakfast on `unpopular-date` -> 0
;; Marissa: attended `popular-date` -> 1
;; Now postgres numbers the IDs starting from 1, so the expected counts are:
(def expected-counts-at-next-date #{{:id 2 :count 2}
                                    {:id 3 :count 0}
                                    {:id 4 :count 1}})

(t/deftest prepare-breakfast-primitives
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
      (t/is (= (list (nth mock-users 1))
               (db-ops/choose-bringers db/db next-date 1))))
    (t/testing "updates the bringer table"
      (t/is (= (list (nth mock-users 1))
               (db/get-bringers-on db/db {:day next-date}))))))

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

(t/deftest prime-attendance
  (prepare-mock-db)
  (db-ops/prime-attendance db/db)
  (t/testing "does not revert previous sign off"
    (let [date  (next-monday)
          email (-> mock-emails first :email)]
      ;; When a person is signed off from an event
      (t/is (= 1 (db/remove-attendance-by-email-at db/db
                                                   {:email email
                                                    :day   date})))
      ;; (check that the person is now really signed off)
      (t/is (not (db-ops/attends-event? email date)))

      ;; When prime-attendance is called with no arguments (background chore)
      (db-ops/prime-attendance db/db)

      ;; The person is still signed off
      (t/is (not (db-ops/attends-event? email date))))))

(t/deftest add-new-member
  (t/testing "can add new member"
    (t/is (some #(= % {:fullname "nat"
                       :email    "natalie.newcomer@company.com"})
                (do (reset-db! db/db)
                    ;; insert mock team
                    (doall (for [user mock-users] (db/insert-member db/db user)))
                    ;; sign them up for ALL THE BREAKFASTS
                    (db-ops/prime-attendance db/db)
                    ;; When we add a new member
                    (t/is (= :success (db-ops/add-new-team-member
                                       "natalie.newcomer@company.com" "nat")))
                    ;; Then they are added to the existing events
                    (db/get-all-attendees db/db {:day (next-monday)})))))
  (t/testing "adding same email twice is an error"
    (prepare-mock-db)
    (let [{:keys [email fullname]}  (first mock-users)
          result (db-ops/add-new-team-member email fullname)]
      (t/is (= :error result)))))

(def random-date (jt/local-date 1980 11 12))

(t/deftest prepare-breakfast
  (prepare-mock-db)
  (t/testing "on a date with no attendees returns nil"
    (t/is (nil? (db-ops/prepare-breakfast db/db random-date 3))))

  (t/testing "on a date with attendees, prepares it"
    (let [attendance-data (db-ops/prepare-breakfast db/db next-date 1)]
      (t/is (some? (:bringer attendance-data)))
      (t/is (some? (:attendees attendance-data)))))

  (t/testing "the bringer and attendees are not changed on next invocation"
    (let [attendance-data (db-ops/prepare-breakfast db/db next-date 1)]
      (t/is (some? (:bringer attendance-data)))
      (t/is (some? (:attendees attendance-data))))))

(t/deftest cancel-breakfast
  (prepare-mock-db)
  (t/testing "given a date where an event is planned"
    (let [date popular-date]
      (t/testing "when the breakfast is canceled"
        (db-ops/cancel-event date)
        (t/testing "then there are no more attendees"
          (let [attendees (db/get-all-attendees db/db {:day date})]
            (t/is empty? attendees)))))))

(t/deftest multiple-bringers
  (prepare-mock-db)
  (t/testing "can choose multiple bringers"
    (let [result (db-ops/choose-bringers db/db next-date 2)]
      (t/is (= 2 (count result)))
      (t/is (= ["catherina.carollo@company.com" "marissa.mucci@company.com"]
               (map :email result)))))
  (t/testing "handles when one of the bringers signs off"
    (let [result (db-ops/safe-remove db/db "marissa.mucci@company.com"
                                     next-date next-date 2)]
      (t/is
       (= [:ok-new-responsible
           (list "catherina.carollo@company.com" "stan.sandiford@company.com")]
          result)))))
