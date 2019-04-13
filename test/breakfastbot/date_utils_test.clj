(ns breakfastbot.date-utils-test
  (:require [breakfastbot.date-utils :as sut]
            [clojure.test :as t]
            [java-time :as jt]))

(t/deftest next-monday
  (t/testing "calling without arguments is the same as calling today's date"
    (t/is (= (sut/next-monday)
             (sut/next-monday (jt/local-date)))))
  (t/testing "calling with local-datetime shaves off the time part"
    (t/is (= (sut/next-monday)
             (sut/next-monday (jt/local-date-time))))))

