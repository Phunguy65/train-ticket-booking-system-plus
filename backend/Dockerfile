# ─── Stage 1: build ──────────────────────────────────────────────────────────
# Context must be the project root so build-logic/ and gradle/libs.versions.toml
# are reachable by the backend Gradle build.
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

# Copy Gradle wrapper and build-metadata files first to maximise layer caching.
# These change far less often than source code.
COPY backend/gradlew backend/gradlew
COPY backend/gradle/ backend/gradle/
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/
COPY build-logic/ build-logic/
COPY gradle/ gradle/
COPY gradle.properties gradle.properties
COPY backend/third-party/ backend/third-party/

RUN chmod +x backend/gradlew

# Pre-resolve dependencies (cached until build files above change).
RUN backend/gradlew -p backend dependencies --no-daemon --quiet

# Copy source and build the executable fat jar.
# Skip tests — they require TestContainers / Docker and are run separately in CI.
COPY backend/src/ backend/src/
RUN backend/gradlew -p backend bootJar --no-daemon -x test

# ─── Stage 2: run ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre

WORKDIR /app

# Use a non-root user for security.
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 --ingroup spring spring

COPY --from=build --chown=spring:spring \
    /workspace/backend/build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
