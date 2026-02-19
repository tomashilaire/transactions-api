## ── Stage 1: build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Gradle wrapper and dependency descriptors first to leverage layer caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet

# Copy source and build the fat JAR
COPY src ./src
RUN ./gradlew bootJar --no-daemon --quiet

## ── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/transactions-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
