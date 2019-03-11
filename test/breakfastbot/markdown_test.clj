(ns breakfastbot.markdown-test
  (:require [breakfastbot.markdown :as sut]
            [clojure.test :as t]))

(t/deftest bullet-list
  (t/testing "Can create bullet list"
    (t/is (= "* Foo\n* Bar\n* Baz"
             (sut/bullet-list ["Foo" "Bar" "Baz"])))))

(t/deftest mention
  (t/testing "Can mention user with message"
    (t/is (= "@**Miles McInnis** hey"
             (sut/mention "Miles McInnis" "hey"))))
  (t/testing "Can mention user without message"
    (t/is (= "@**Firstname Lastname**"
             (sut/mention "Firstname Lastname")))))
