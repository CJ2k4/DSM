# syntax=docker/dockerfile:1
# Build from the REPO ROOT (the Maven wrapper lives there):
#   docker build -f backend/Dockerfile .

# ---- Build stage ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY mvnw ./
COPY .mvn .mvn
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src

# One cached step: /root/.m2 keeps both the wrapper's Maven distro and all
# dependencies warm between builds.
RUN --mount=type=cache,target=/root/.m2 \
    sh mvnw -f backend/pom.xml package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-jammy

# curl is only here for the compose healthcheck (the JRE image ships without it).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home appuser

WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar

USER appuser
EXPOSE 8080

# MaxRAMPercentage keeps the heap sane in small containers (e.g. Render free tier).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -XX:MaxRAMPercentage=75.0 -jar /app/app.jar"]
