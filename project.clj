(defproject breakfastbot "1.0"
  :description "Breakfast organizer bot for Zulip"
  :url "https://github.com/studer-l/breakfastbot"
  :license {:name "BSD-3-Clause"
            :url  "https://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 [mount "0.1.16"]
                 [tolitius/mount-up "0.1.2"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.postgresql/postgresql "42.2.6"]
                 [com.layerware/hugsql "0.4.9"]
                 [org.clojure/tools.logging "0.4.1"]
                 [clojure.java-time "0.3.2"]
                 [com.mchange/c3p0 "0.9.5.4"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [migratus "1.2.3"]
                 [org.clojars.studerl/clojure-zulip "0.4.1"]]
  :plugins [[lein-cloverage "1.1.1"]]
  :cloverage {:html? false}
  :resource-paths ["resources"]
  :main breakfastbot.core
  :profiles {:dev     {:jvm-opts ["-Dconf=dev-config.edn"]}
             :test    {:jvm-opts ["-Dconf=test-config.edn"]}
             :uberjar {:jar-name     "breakfastbot.jar"
                       :uberjar-name "breakfastbot-standalone.jar"
                       :aot          :all}})
