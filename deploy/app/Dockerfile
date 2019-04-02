FROM clojure:lein-alpine
WORKDIR /breakfastbot
COPY . .
RUN LEIN_SNAPSHOTS_IN_RELEASE=true lein uberjar

FROM openjdk:8-alpine
WORKDIR /breakfastbot
COPY --from=0 /breakfastbot/target/breakfastbot-standalone.jar app.jar
ENTRYPOINT ["java", "-Dconf=prod-config.edn", "-jar", "app.jar"]
