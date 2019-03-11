(ns breakfastbot.config
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :refer [info]]
            [mount.core :refer [defstate]]))

(defn read-config []
  (if-let [conffile (System/getProperty "conf")]
    (do
      (info "Loading config" conffile)
      (-> conffile slurp edn/read-string))
    (throw (ex-info "No config file passed as property" {}))))

(defstate config
  :start (read-config))
