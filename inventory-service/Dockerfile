# Multi-stage build so only Docker is required (no local Maven/JDK).
# Build context is the repo root (see docker-compose.yml).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY common ./common
COPY common-web ./common-web
COPY inventory-service ./inventory-service
RUN mvn -B -pl inventory-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/inventory-service/target/inventory-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
