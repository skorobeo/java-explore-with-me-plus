FROM eclipse-temurin:21-jre
COPY target/ewm-stats-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]