(ns breakfastbot.markdown
  (:require [clojure.string :as s]))

(defn bullet-list-item [depth item]
  (str (apply str (repeat depth " ")) "* " item))

(defn bullet-list [items]
  (->> items
       (map #(bullet-list-item 0 %))
       (s/join "\n")))

(defn mention
  ([full-name]
   (str "@**" full-name "**"))
  ([full-name msg]
   (str "@**" full-name "** " msg)))
