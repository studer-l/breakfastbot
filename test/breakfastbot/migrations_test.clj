(ns breakfastbot.migrations-test
  (:require [clojure.test :as t]
            [migratus.core :as migratus]
            [mount.core :as mount]
            [breakfastbot.db-test :refer [drop-db! populate-db!]]
            [breakfastbot.core :refer [as-migratus-config ensure-db-migrated]]
            [breakfastbot.config :refer [config]]
            [breakfastbot.db :as db]))

(mount/start #'db/db #'config)

(t/deftest migrations
  (drop-db! db/db)
  (let [migratus-config (as-migratus-config config)]
    (t/testing "can apply"
      (t/is nil? (ensure-db-migrated config)))
    (t/testing "can fill in some mock data"
      (populate-db!))
    (t/testing "can un-apply migrations again"
      (t/is (nil? (migratus/rollback-until-just-after migratus-config 20190331203037)))
      (t/is (nil? (migratus/down migratus-config [20190331203037]))))
    (t/testing "can re-apply migrations"
      (t/is (nil? (ensure-db-migrated config))))))
