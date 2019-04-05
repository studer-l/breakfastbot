(ns breakfastbot.db
  (:require [breakfastbot.config :refer [config]]
            [hugsql.core :as hugsql]
            [mount.core :as mount :refer [defstate]])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))

(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(defn mk-pool [db-spec] (pool db-spec))

(defstate db
  :start (do ;; strange bug: config is not loaded
           (mount/start #'config)
           (mk-pool (:db config)))
  :stop (.close (:datasource db)))

(hugsql/def-db-fns "breakfastbot/sql/members.sql")
(hugsql/def-db-fns "breakfastbot/sql/attendances.sql")
(hugsql/def-db-fns "breakfastbot/sql/bringer.sql")
(hugsql/def-db-fns "breakfastbot/sql/app_state.sql")
