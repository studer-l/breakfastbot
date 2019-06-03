(ns breakfastbot.chatting-test
  (:require [breakfastbot.chatting :as sut]
            [clojure.test :as t]
            [clojure.core.async :as a]
            [java-time :as jt]))

(def global (atom :nothing))

(defn set-global! [val]
  (reset! global val))

(t/deftest add-sync-handler
  (reset! global nil)
  (let [done-cb (a/chan)
        producer-ch (a/chan)
        kill-ch (sut/add-sync-handler producer-ch
                                      (fn [value] (reset! global value)
                                        (a/>!! done-cb :done)))]
    (t/testing "sync handler is called"
      (t/is (= nil @global))
      (a/>!! producer-ch :something)
      ;; wait for done-cb to signal that value was overriden, beware deadlock...
      (t/is (= :done (a/<!! done-cb)))
      (t/is (= :something @global))
      ;; finally close the handler
      (a/>!! kill-ch :stop))))

(t/deftest date->subject
  (t/testing "matches channel subject"
    (t/is (= "Breakfast 3.1.2018"
             (sut/date->subject (jt/local-date 2018 1 3))))))
