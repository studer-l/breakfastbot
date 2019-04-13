(ns breakfastbot.announcement-test
  (:require [breakfastbot.announcement :as sut]
            [clojure.string :as s]
            [clojure.test :as t]
            [java-time :as jt]
            [breakfastbot.db-test :refer [prepare-mock-db]]
            [breakfastbot.db-ops :as db-ops]))

(def mock-data
  {:bringer  {:fullname "Jimmy McGill"}
   :attendees [{:fullname "Jimmy McGill"}
               {:fullname "Joey Dixon"}
               {:fullname "Drama Girl"}
               {:fullname "Sound Guy"}]})

(t/deftest announcement
  (t/testing "is correctly formated"
    (t/is (= (s/join "\n" ["🤖📣 BREAKFAST SCHEDULED 🤖📣"
                           ""
                           "Attendees:"
                           "* @**Jimmy McGill**"
                           "* @**Joey Dixon**"
                           "* @**Drama Girl**"
                           "* @**Sound Guy**"
                           "Total attendees: 4"
                           ""
                           "Responsible for bringing Breakfast: @**Jimmy McGill**"])
             (sut/announce-breakfast-message mock-data)))))

(def monday (jt/local-date 2019 4 8))
(def friday-morning (jt/local-date-time 2019 4 5 9))
(def friday-noon (jt/local-date-time 2019 4 5 13))
(def saturday (jt/local-date 2019 4 6))

(t/deftest time-to-announce
  (t/testing "do not announce on monday"
    (t/is (nil? (sut/time-to-announce? monday))))
  (t/testing "do not announce on friday morning yet"
    (t/is (nil? (sut/time-to-announce? friday-morning))))
  (t/testing "do announce on friday after noon"
    (t/is (some? (sut/time-to-announce? friday-noon))))
  (t/testing "do announce on saturday"
    (t/is (some? (sut/time-to-announce? saturday))))
  (t/testing "do not announce on friday at midnight"
    (t/is (nil? (sut/time-to-announce? (jt/local-date-time 2019 4 12 0 11))))))

(t/deftest schedule-breakfast
  ;; Given there is a breakfast with attendees next monday
  (prepare-mock-db)
  (db-ops/prime-breakfast monday)
  (t/testing "On friday morning, no breakfast is scheduled yet"
    (t/is (nil? (sut/schedule-breakfast friday-morning))))
  (t/testing "On friday after noon, the breakfast is scheduled"
    (let [[event-date attendance-data] (sut/schedule-breakfast friday-noon)]
      (t/is (= event-date monday))
      (t/is (some? (:bringer attendance-data)))
      (t/is (some? (:attendees attendance-data))))))
