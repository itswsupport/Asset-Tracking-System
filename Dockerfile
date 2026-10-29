# syntax=docker/dockerfile:1

# AMS backend (Asset-Tracking-System) — Spring Boot 4.1 on Java 21, packaged as an
# executable jar and run with `java -jar`. Same shape as the TMS backend image
# (Maven builder -> slim JRE runner), with the versions this project actually uses:
# java.version=21 in pom.xml, where TMS is Java 8.
#
# Build:
#   docker build -t ams-backend .
# The jar is built inside the image, so no `./mvnw package` beforehand.

# ---------------------------------------------------------------------------
# builder
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml ./
COPY src ./src

# Tests are skipped deliberately: the only test is AssetTrackingSystemApplicationTests, a
# bare @SpringBootTest contextLoads() that starts the whole Spring context and so opens the
# datasource at jdbc:mysql://192.168.100.30:3309/db_asset. A build machine cannot be assumed
# to reach it, and a build that fails for lack of a database says nothing about the code.
#
# The cache mount keeps ~/.m2 between builds, so only changed dependencies are fetched.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# artifactId=Asset-Tracking-System, version=0.0.1-SNAPSHOT
#   -> target/Asset-Tracking-System-0.0.1-SNAPSHOT.jar (the executable one, ~60MB).
# Renamed so the runtime stage does not have to track the version. The sibling
# .jar.original that spring-boot-maven-plugin leaves behind does not match *.jar.
RUN cp target/Asset-Tracking-System-*.jar /build/app.jar

# ---------------------------------------------------------------------------
# runner
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

LABEL maintainer="ruchatest4@gmail.com"
LABEL description="AMS (Asset Tracking System) Spring Boot backend"
LABEL version="1.0"

# curl for the healthcheck; tzdata because Alpine ships no zoneinfo database and without it
# the TZ below is silently ignored and the JVM stays on UTC.
RUN apk add --no-cache curl tzdata

# The application stamps and compares dates with the JVM clock (warranty status, device
# age, added/updated dates). A container defaults to UTC, 5h30m behind IST — enough to put
# yesterday's date on an evening record.
ENV TZ=Asia/Kolkata

# Spring's default port — application.properties sets no server.port. The UI proxies to
# http://ams-backend:8080 (BACKEND_ORIGIN in the AMS_UI Jenkinsfile), so the two must match.
ENV SERVER_PORT=8080

# Heap as a percentage of the container's memory limit (the Jenkinsfile runs it with -m).
# 75.0 with the decimal, as in the TMS image — Java 21 also accepts 75, but the decimal form
# works on every JDK and keeps the two images alike. egd: non-blocking SecureRandom, so
# startup does not stall on a container with little entropy.
#
# No -XX:+UseContainerSupport, unlike the TMS image: on Java 21 it is already on by default,
# and the flag exists only on Linux JVMs — the same line run on a Windows JDK dies with
# "Unrecognized VM option" before Spring starts.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run as a non-root user.
RUN addgroup -S spring && adduser -S spring -G spring

# No /upload here and no volume in the Jenkinsfile, unlike TMS: the app writes no files (bulk
# upload sends the parsed rows as JSON). If file uploads are ever added, bind-mount the host's
# /upload/ams (and ONLY that folder) the way the TMS pipeline mounts /upload — never create or
# chown the shared /upload from here.

COPY --from=builder --chown=spring:spring /build/app.jar ./app.jar

USER spring
EXPOSE 8080

# Database credentials are NOT baked in — Spring's relaxed binding overrides
# application.properties at run time, e.g.
#   docker run -e SPRING_DATASOURCE_URL=... -e SPRING_DATASOURCE_PASSWORD=... \
#              -e LOGIN_PASSWORD_VERIFY=true ...
# See docker-compose.yml and the Jenkinsfile Deploy stage.

# There is no health endpoint, and every API but /login and /logout needs a sign-in token
# (JWT), so the probe signs in as employee 0 — a code that does not exist. /login is open,
# and looking that code up is a real database query:
#
#   "status_code":500  "User Not Found" — the query RAN, the database is reachable  → healthy
#   "status_code":501  the query failed — the database is unreachable              → unhealthy
#
# The BODY is checked, not just curl -f: this backend answers HTTP 200 for everything and
# puts the outcome in "status_code". curl -f alone would call a container whose database is
# unreachable healthy.
#
# 127.0.0.1 rather than localhost, so the probe cannot resolve to ::1 and miss a server
# bound only on IPv4.
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
   CMD curl -fsS -X POST "http://127.0.0.1:${SERVER_PORT}/login/?emp_code=0&emp_pass=healthcheck" | grep -Eq '"status_code" ?: ?500' || exit 1

# sh -c so JAVA_OPTS is word-split by the shell rather than passed as one argument.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
