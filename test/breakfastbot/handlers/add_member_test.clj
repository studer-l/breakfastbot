(ns breakfastbot.handlers.add-member-test
  (:require [breakfastbot.handlers.add-member :as sut]
            [clojure.test :as t]))

(t/deftest parse-add-member
  (t/testing "matches against emails"
    (t/is (= "first.name@company.com"
             (sut/parse-add-member "ignored" "add first.name@company.com"))))
  (t/testing "does not match against name"
    (t/is (nil? (sut/parse-add-member "ignored" "add first name")))))
