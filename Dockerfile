# syntax=docker/dockerfile:1
#
# Build stage: resolve the dependencies and compile.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# The dependency layer is cached separately from the sources, so a source-only change
# does not re-download the world.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime stage: a JRE and the jar. No compiler, no Maven, no shell history.
FROM eclipse-temurin:25-jre-jammy AS runtime

RUN groupadd --system --gid 1001 seo \
 && useradd --system --uid 1001 --gid seo --home-dir /app --shell /usr/sbin/nologin seo \
 && mkdir -p /app/data /app/.secrets \
 && chown -R seo:seo /app

WORKDIR /app
COPY --from=build /build/target/seo-0.1.0.jar /app/seo.jar

USER seo
EXPOSE 8083

ENV SERVER_PORT=8083 \
    SECRET_STORE_PATH=/app/.secrets \
    UPLOAD_DIR=/app/data \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD ["/bin/sh", "-c", "exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT} && printf 'GET /actuator/health HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && head -1 <&3 | grep -q '200'"]

ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS -jar /app/seo.jar"]
