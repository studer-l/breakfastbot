(ns breakfastbot.handlers.cancel-test
  (:require [breakfastbot.handlers.cancel :as sut]
            [clojure.test :as t]
            [java-time :as jt]))

(t/deftest parse-cancel
  (t/testing "when no date is given"
    (let [res (sut/parse-cancel "somebody" "cancel")]
      (t/testing "then next monday is chosen"
        (t/is res)
        (t/is (instance? java.time.LocalDate res)))))
  (t/testing "when a date is given"
    (let [res (sut/parse-cancel "???" "cancel 1.1.2020")]
      (t/testing "then the date is extracted"
        (t/is (= res (jt/local-date 2020 1 1)))))))
