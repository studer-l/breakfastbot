(ns breakfastbot.handlers.add-member-test
  (:require [breakfastbot.handlers.add-member :as sut]
            [clojure.test :as t]
            [breakfastbot.handlers.common :refer [answers]]))

(t/deftest parse-add-member
  (t/testing "matches against emails"
    (t/is (= "first.name@company.com"
             (sut/parse-add-member "ignored" "add first.name@company.com"))))
  (t/testing "does not match against name"
    (t/is (nil? (sut/parse-add-member "ignored" "add first name")))))

(t/deftest add-member-answer
  (t/testing "success answer is festive"
    (t/is (= "🎉🎈 Welcome @**Saul Goodman**!! 🎉🎈"
             ((:welcome answers) "Saul Goodman")))))
