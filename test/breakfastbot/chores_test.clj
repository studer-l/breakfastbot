(ns breakfastbot.chores-test
  (:require [breakfastbot.chores :as sut]
            [clojure.test :as t]
            [clojure.core.async :as a]))

;; testing an async endless loop... this is going to be funky
;; basic idea: Hand a "test verification" channel into the function that gets
;; called repeatedly to report back

(defn report-channel [msecs]
  (let [c (a/chan)]
    [(sut/repeatedly-async-call msecs (fn [] (a/>!! c "still alive"))) c]))

(t/deftest repeatedly-async-call
  (t/testing "is calling once every period"
    (let [[kill outp] (report-channel 50)]
      (dotimes [_ 10]
        ;; test by asserting that answer arrives prior to timeout
        (let [[_ c] (a/alts!! [outp (a/timeout 100)])]
          (t/is (= c outp))))
      ;; stop it
      (t/testing "can be killed via kill-channel"
        (a/>!! kill :stop)

        ;; ensuire it stopped
        (let [[_ c] (a/alts!! [outp (a/timeout 100)])]
          (t/is (not= c outp)))))))
