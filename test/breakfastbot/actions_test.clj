(ns breakfastbot.actions-test
  (:require [breakfastbot.actions :as sut]
            [breakfastbot.handlers.error :refer [bb-error-handler]]
            [breakfastbot.handlers.help :refer [handlers->help-handler]]
            [clojure.test :as t]))

(def greedy-handler
  {:matcher (fn [author message] [author message])
   :action (fn [[author message]] [:greedy-handler author message])
   :help "matches everything"})

(def non-handler
  "Never matches anything"
  {:matcher (fn [author message] nil)
   :action (fn [[author message]] [:non-handler author message])
   :help "matches nothing"})

(t/deftest try-handler
  (t/testing "returning true in matcher leads to action being called"
    (t/is (= [:greedy-handler "a@b.c" "foobar"]
             (sut/try-handler greedy-handler "a@b.c" "foobar"))))
  (t/testing "returning nil in matcher does not trigger action"
    (t/is (nil? (sut/try-handler non-handler "a@b.c" "foobar")))))

(t/deftest dispatch-handlers
  (t/testing "ignores non-trigger inputs"
    (t/is (nil? (sut/dispatch-handlers [non-handler greedy-handler]
                                       "Flume" "Jewel"))))
  (t/testing "matches trigger sequence"
    (t/is (= [:greedy-handler "a@b.c" "quux"]
             (sut/dispatch-handlers [non-handler greedy-handler]
                                    "a@b.c" "@**Breakfast Bot** quux"))))

  (t/testing "matches trigger sequence removing space"
    (t/is (= [:greedy-handler "a@b.c" "baz"]
             (sut/dispatch-handlers [non-handler greedy-handler]
                                    "a@b.c" "@**Breakfast Bot**    baz")))))

(t/deftest test-bb-error-handler
  (let [handlers [non-handler non-handler bb-error-handler]]
    (t/testing "When sending some random message, it is ignored"
      (t/is
       (nil?
        (sut/dispatch-handlers handlers "a@b.c"
                               "whatever @**Breakfast Bot** is a fun dude"))))
    (t/testing (str "When a message starting with the trigger message is sent, "
                    "but no handler matches, a  helpful message is sent")
      (t/is (some?
             (sut/dispatch-handlers handlers "a@b.c"
                                    "@**Breakfast Bot** send help"))))))

(t/deftest help-message
  (let [handlers [non-handler non-handler]
        handlers (conj handlers (handlers->help-handler handlers))]
    (t/is (= {:direct-reply "matches nothing\nmatches nothing"}
             (sut/dispatch-handlers handlers "me@company.com"
                                    "@**Breakfast Bot** help")))))
