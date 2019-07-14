(ns breakfastbot.refresh-names-test
  (:require [breakfastbot.refresh-names :as sut]
            [clojure.test :as t]
            [mount.core :as mount]
            [breakfastbot.db :as db]
            [breakfastbot.db-test :refer [prepare-mock-db]]))

(mount/start #'db/db)

(def mock-reply-ok
  {:result  "success"
   :members [{:is_admin false :email "a@b.c" :full_name "abc"}
             {:is_admin false :email "uml@u.t" :full_name "ümláut"}]})

(t/deftest parse-reply
  (t/testing "converts reply to map email → fullname"
    (t/is (= {"a@b.c"   "abc"
              "uml@u.t" "ümláut"}
             (sut/parse-reply mock-reply-ok)))))

(def mock-member-update
  "Changes the name of Stan to contain umlauts"
  {"stan.sandiford@company.com"    "Stan Sándïford"
   "catherina.carollo@company.com" "Cahterina Carollo"
   "miles.mcinnis@company.com"     "Miles McInnis"
   "marissa.mucci@company.com"     "Marisssa Mucci"})

(t/deftest difference-zulip-and-db
  (t/testing "Finds set of changed members"
    (prepare-mock-db)
    (t/is (= '(["stan.sandiford@company.com" "Stan Sándïford"])
             (sut/difference-zulip-and-db (db/get-members db/db)
                                          mock-member-update))))
  (t/testing "handles nil"
    (t/is (empty? (sut/difference-zulip-and-db (db/get-members db/db)
                                               nil)))))

(def mock-reply-ok2
  {:result "success"
   :members
   [{:email "catherina.carollo@company.com" :full_name "Cahterina Carôllo"}
    {:email "ignored@company.com" :full_name "who's this guy"}]})

(def mock-reply-error {:result "error" :message "bad transistor day"})

(t/deftest refresh-names
  (t/testing "Mutates db"
    (prepare-mock-db)
    (t/testing "updating one record"
      (t/is (= '(1) (sut/refresh-names db/db mock-reply-ok2))))
    (t/testing "changes one name"
      (t/is (= "Cahterina Carôllo"
               (:fullname (db/get-member-by-email
                           db/db
                           {:email "catherina.carollo@company.com"}))))))
  (t/testing "Aborts on error"
    (t/is (empty? (sut/refresh-names db/db mock-reply-error)))))
