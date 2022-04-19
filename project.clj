(defproject breakfastbot "1.1"
  :description "Breakfast organizer bot for Zulip"
  :url "https://github.com/studer-l/breakfastbot"
  :license {:name "BSD-3-Clause"
            :url  "https://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.5.648"]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.3"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.postgresql/postgresql "42.3.3"]
                 [com.layerware/hugsql "0.5.3"]
                 [org.clojure/tools.logging "1.2.4"]
                 [clojure.java-time "0.3.2"]
                 [com.mchange/c3p0 "0.9.5.5"]
                 [ch.qos.logback/logback-classic "1.2.11"]
                 [migratus "1.3.6"]
                 [org.clojars.studerl/clojure-zulip "0.4.5"]]
  :plugins [[lein-cloverage "1.1.1"]]
  :cloverage {:html? false}
  :resource-paths ["resources"]
  :main breakfastbot.core
  :profiles {:dev     {:jvm-opts ["-Dconf=dev-config.edn"]}
             :test    {:jvm-opts ["-Dconf=test-config.edn"]}
             :uberjar {:jar-name     "breakfastbot.jar"
                       :uberjar-name "breakfastbot-standalone.jar"
                       :aot          :all}})
