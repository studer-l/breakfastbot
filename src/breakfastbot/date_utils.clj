(ns breakfastbot.date-utils
  (:require [java-time :as jt]))

(defn next-monday
  ([] (next-monday (jt/local-date)))
  ([after-date]
   (jt/adjust (jt/local-date after-date) :next-or-same-day-of-week :monday)))

(defn mondays
  "Stream of Mondays"
  ([]  ;; from today
   (mondays (next-monday)))
  ([from-date]
   (jt/iterate jt/adjust (next-monday from-date) :next-day-of-week :monday))
  ([from-date to-date]
   (take-while #(jt/after? to-date %) (mondays from-date))))
