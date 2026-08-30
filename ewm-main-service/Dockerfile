FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -pl ewm-main-service -am -DskipTests package

FROM eclipse-temurin:21-jre
COPY --from=build /app/ewm-main-service/target/ewm-main-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]