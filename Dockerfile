FROM eclipse-temurin:21-jre-alpine-3.24

WORKDIR /app

COPY target/nanobank-identity-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]