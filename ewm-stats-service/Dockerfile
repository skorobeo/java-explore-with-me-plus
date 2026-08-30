FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -pl ewm-stats-service -am -DskipTests package

FROM eclipse-temurin:21-jre
COPY --from=build /app/ewm-stats-service/target/ewm-stats-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]