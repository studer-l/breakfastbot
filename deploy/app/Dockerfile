FROM openjdk:8-alpine
WORKDIR /breakfastbot
COPY target/breakfastbot-standalone.jar app.jar
ENTRYPOINT ["java", "-Dconf=prod-config.edn", "-jar", "app.jar"]
