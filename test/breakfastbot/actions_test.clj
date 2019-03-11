(ns breakfastbot.actions-test
  (:require [breakfastbot.actions :as sut]
            [clojure.test :as t]))

(def greedy-handler
  {:matcher (fn [author message] [author message])
   :action (fn [[author message]] [:greedy-handler author message])
   :help nil})

(def non-handler
  "Never matches anything"
  {:matcher (fn [author message] nil)
   :action (fn [[author message]] [:greedy-handler author message])
   :help nil})

(t/deftest try-handler
  (t/testing "returning true in matcher leads to action being called"
    (t/is (= [:greedy-handler "a@b.c" "foobar"]
             (sut/try-handler greedy-handler "a@b.c" "foobar"))))
  (t/testing "returning nil in matcher does not trigger action"
    (t/is (nil? (sut/try-handler non-handler "a@b.c" "foobar")))))
