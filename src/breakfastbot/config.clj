(ns breakfastbot.config
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :refer [info]]
            [mount.core :refer [defstate]])
  (:import (org.slf4j LoggerFactory)
           (ch.qos.logback.classic Logger Level)))

(defn- ->logback-level [level]
  (case level
    :trace Level/TRACE
    :debug Level/DEBUG
    :info Level/INFO
    :warn Level/WARN
    :error Level/ERROR))

(defn configure-logger
  "Sets root logging level"
  [name level]
  (-> name
      LoggerFactory/getLogger
      (.setLevel (->logback-level level))))

(defn read-config []
  (if-let [conffile (System/getProperty "conf")]
    (do
      (info "Loading config" conffile)
      (let [config (-> conffile slurp edn/read-string)]
        ;; configure global logging, defaults are in logback.xml
        (if-let [level (-> config :logging :root)]
          (configure-logger Logger/ROOT_LOGGER_NAME level))
        (if-let [level (-> config :logging :breakfastbot)]
          (configure-logger "breakfastbot" level))
        config))
    (throw (ex-info "No config file passed as property" {}))))

(defstate config
  :start (read-config))
