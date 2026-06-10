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

# Install order-contracts
COPY services/order-contracts/pom.xml services/order-contracts/pom.xml
COPY services/order-contracts/src services/order-contracts/src
RUN mvn -f services/order-contracts/pom.xml install -DskipTests -B

# Build notification-service
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/notification-service/src services/notification-service/src
RUN mvn -f services/notification-service/pom.xml package -DskipTests -B

# ============================================================
# Stage 2 — Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="zatadev"
LABEL application="notification-service"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /workspace/services/notification-service/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

ENV RABBITMQ_HOST=localhost \
    RABBITMQ_PORT=5672 \
    RABBITMQ_USER=guest \
    RABBITMQ_PASSWORD=guest \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider \
    http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
