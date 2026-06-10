# ============================================================
# Stage 1 — Builder
# ============================================================
FROM maven:3.9.15-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace

# Install root POM (no recursive — just registers the parent)
COPY pom.xml pom.xml
RUN mvn -f pom.xml install -N -B

# Install common
COPY services/common/pom.xml services/common/pom.xml
COPY services/common/src services/common/src
RUN mvn -f services/common/pom.xml install -DskipTests -B

# Build user-service
COPY services/user-service/pom.xml services/user-service/pom.xml
COPY services/user-service/src services/user-service/src
RUN mvn -f services/user-service/pom.xml package -DskipTests -B

# ============================================================
# Stage 2 — Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="zatadev"
LABEL application="user-service"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /workspace/services/user-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

ENV DB_URL=jdbc:postgresql://localhost:5432/userdb \
    DB_USERNAME=postgres \
    DB_PASSWORD=postgres \
    JWT_SECRET=change-me-in-production-minimum-32-chars \
    JWT_EXPIRATION_MS=900000 \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider \
    http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
