# ============================================================
# Stage 1 — Builder
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY services/user-service/mvnw services/user-service/mvnw
COPY services/user-service/.mvn services/user-service/.mvn
RUN chmod +x services/user-service/mvnw

COPY pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    services/user-service/mvnw -f pom.xml install -N -B

COPY services/notification-service/pom.xml services/notification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    services/user-service/mvnw -f services/notification-service/pom.xml dependency:go-offline -B

COPY services/notification-service/src services/notification-service/src
RUN --mount=type=cache,target=/root/.m2 \
    services/user-service/mvnw -f services/notification-service/pom.xml package -DskipTests -B

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
