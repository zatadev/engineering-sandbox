# Runbook — Service Down Incident Response

**Version:** 1.0  
**Scope:** user-service, order-service, notification-service  
**Stack:** Prometheus · Grafana · Loki · Tempo  
**Last updated:** 2026-06-18

---

## Overview

Use this runbook when a `ServiceDown` alert fires. It walks through detection, diagnosis, resolution, and post-incident verification using Prometheus, Grafana, Loki, and Tempo.

**Estimated resolution time:** 5–15 minutes for a known cause, 30–60 minutes for an unknown root cause.

---

## Alert Definition

```
Alert:    ServiceDown
Severity: critical
Expr:     up{job=~"user-service|order-service|notification-service"} == 0
For:      1 minute
```

The alert fires when a service's `/actuator/health` endpoint becomes unreachable for more than 1 minute. Grafana sends a webhook notification to the configured contact point.

---

## Step 1 — Detect the alert

### 1.1 Incoming notification

When `ServiceDown` fires, the configured webhook receives a POST:

```json
{
  "status": "firing",
  "alerts": [{
    "labels": {
      "alertname": "ServiceDown",
      "job": "order-service",
      "instance": "order-service:8081",
      "severity": "critical"
    },
    "annotations": {
      "summary": "Service order-service is down",
      "description": "order-service:8081 has been unreachable for more than 1 minute"
    }
  }]
}
```

Key fields: `job` (which service), `instance` (host:port), `startsAt` (when it started).

### 1.2 Grafana Alerting dashboard

Navigate to **Grafana → Alerting → Alert rules**.

Confirm the rule state:
- `Firing` — service is currently down
- `Pending` — condition met but `for` duration not elapsed yet
- `Normal` — alert resolved

Open the alert to see the generatorURL link to the source panel.

---

## Step 2 — Identify the failing service via metrics

### 2.1 Prometheus targets

Open **http://localhost:9090/targets**

Filter by the affected service. A down target shows:
- State: `DOWN`
- Error: `context deadline exceeded` (timeout) or `connection refused`

This confirms the service is unreachable at the network level, not just unhealthy.

### 2.2 Grafana — HTTP Metrics dashboard

Open **Grafana → Dashboards → Sandbox → HTTP Metrics**

Look for:
- **HTTP Request Rate** — flatline on the affected service (no traffic being processed)
- **HTTP Error Rate** — spike in 5xx immediately before the flatline (may indicate a crash loop)
- **HTTP Latency p99** — spike before the outage (possible timeout cascade)

### 2.3 Grafana — JVM Metrics dashboard

Open **Grafana → Dashboards → Sandbox → JVM Metrics**

Look for:
- **JVM Heap Memory** — line drops to zero (process exited) or spikes to max before crash (OOM)
- **JVM Threads** — sudden drop to zero (process exited)
- **GC Pause Rate** — high GC activity immediately before the incident (GC pressure → OOM)

### 2.4 Grafana — Business Metrics dashboard

Open **Grafana → Dashboards → Sandbox → Business Metrics**

Look for:
- **Orders Created Rate** — flatline if order-service is down
- **Notifications Sent Rate** — flatline if notification-service is down or if order-service is not producing events
- **RabbitMQ DLQ Depth** — increase if notification-service is down and messages are piling up in the dead-letter queue

---

## Step 3 — Investigate logs in Loki

### 3.1 Open Grafana Explore — Loki

Navigate to **Grafana → Explore → select Loki datasource**

### 3.2 Query ERROR logs around the incident time

Adjust the time range to cover the 5 minutes before and after `startsAt`.

```logql
{service="order-service"} | json | level="ERROR"
```

Replace `order-service` with the affected service.

### 3.3 Look for crash indicators

```logql
# OutOfMemoryError
{service="order-service"} | json | message =~ ".*OutOfMemoryError.*"

# Connection refused (database, RabbitMQ, Kafka)
{service="order-service"} | json | message =~ ".*Connection refused.*"

# Application failed to start
{service="order-service"} | json | message =~ ".*Application run failed.*"

# Flyway migration failure
{service="order-service"} | json | message =~ ".*FlywayException.*"
```

### 3.4 Check the last log lines before silence

Sort descending by time. The last log entry before the service stopped responding often contains the root cause:

- `Exception in thread "main"` → startup failure
- `java.lang.OutOfMemoryError: Java heap space` → OOM crash
- `HikariPool-1 - Connection is not available` → database connection pool exhausted
- `org.springframework.amqp.AmqpConnectException` → RabbitMQ unreachable

### 3.5 Correlate with correlationId

If a specific request caused the crash, note the `correlationId` from the error log and search for the full request lifecycle:

```logql
{service="order-service"} | json | correlationId="<correlation-id-value>"
```

---

## Step 4 — Follow the trace in Tempo

### 4.1 Extract traceId from logs

In the Loki log panel, expand an error log line. If a `traceId` field is present, a **Tempo** button appears. Click it to jump directly to the trace.

Alternatively, copy the `traceId` value from the JSON log and search in Tempo:

**Grafana → Explore → Tempo → TraceQL**

```
{ traceId = "<trace-id-value>" }
```

### 4.2 Analyse the trace spans

Look for:
- **Long spans** — a span taking significantly longer than usual indicates a timeout or slow dependency
- **Error spans** — spans with `status = ERROR` pinpoint the exact operation that failed
- **Missing spans** — if the trace stops at `order-service` and no `notification-service` span exists, the message was never published (crash before publish) or never consumed (notification-service down)

### 4.3 Identify the failure point

| Observation | Likely root cause |
|---|---|
| Trace ends at HTTP span, no DB span | Database unreachable or connection pool exhausted |
| DB span present, no RabbitMQ publish span | Exception thrown after DB write, before publish |
| RabbitMQ span present, no Kafka span | Kafka unreachable |
| All order-service spans present, no notification-service span | notification-service is down |
| Notification-service span present, error status | Consumer processing failure, check DLQ |

---

## Step 5 — Identify the root cause

### 5.1 Common root causes and indicators

**OOM (Out of Memory)**
- JVM Heap graph: spike to 100% then drop to zero
- Last log: `java.lang.OutOfMemoryError: Java heap space`
- Resolution: restart service, investigate memory leak, increase heap if needed

**Database connection failure**
- Last log: `Unable to acquire JDBC Connection` or `HikariPool-1 - Connection is not available`
- Check: `docker compose ps postgres` — is PostgreSQL healthy?
- Resolution: restart postgres if down, or increase HikariCP pool size if exhausted

**RabbitMQ/Kafka unreachable**
- Last log: `AmqpConnectException` or `org.apache.kafka.common.errors.TimeoutException`
- Check: `docker compose ps rabbitmq kafka`
- Resolution: restart the messaging broker

**Application startup failure**
- Last log: `Application run failed` with stack trace
- Causes: configuration error, Flyway migration failure, missing environment variable
- Resolution: check environment variables, fix migration, check config

**Container killed by OOM killer (Docker)**
- No graceful shutdown log, service just stops
- Check: `docker inspect <container> | grep OOMKilled`
- Resolution: increase Docker memory limits or JVM heap settings

### 5.2 Docker container inspection

```bash
# Check container status and exit code
docker compose ps

# Check if OOM killed
docker inspect sandbox-order-service | grep -A5 '"State"'

# Check recent container logs (last 100 lines)
docker logs sandbox-order-service --tail 100

# Check container resource usage at time of incident
docker stats sandbox-order-service --no-stream
```

---

## Step 6 — Resolve and verify

### 6.1 Resolution actions

**If service crashed (OOM, unhandled exception):**
```bash
docker compose up -d order-service
```

**If dependency is down (database, broker):**
```bash
# Restart the dependency first
docker compose up -d postgres   # or rabbitmq, kafka

# Then restart the service
docker compose up -d order-service
```

**If configuration error:**
```bash
# Fix the configuration, then rebuild and restart
docker compose up -d --build order-service
```

### 6.2 Verify resolution

**Prometheus targets:**
```
http://localhost:9090/targets
```
Target for the recovered service must show `UP`.

**Grafana alert:**

Navigate to **Grafana → Alerting → Alert rules**.
`ServiceDown` must return to `Normal` state.

**Webhook notification:**

webhook.site receives a `"status":"resolved"` POST confirming Grafana detected the recovery:

```json
{
  "status": "resolved",
  "alerts": [{
    "labels": { "job": "order-service" },
    "endsAt": "2026-06-18T10:16:30Z"
  }]
}
```

**End-to-end functional test:**

```bash
# Obtain a token
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.accessToken')

# Create an order — verifies order-service, RabbitMQ, Kafka, and notification-service
curl -s -X POST http://localhost:8081/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":"00000000-0000-0000-0000-000000000001","productId":"00000000-0000-0000-0000-000000000002","quantity":1,"totalPrice":10.00}'
```

Expected: HTTP 201 with order JSON body.

**Verify DLQ is empty:**

Open **Grafana → Dashboards → Sandbox → Business Metrics → RabbitMQ DLQ Depth**.
Value must be 0. A non-zero value means messages accumulated during the outage and may require manual reprocessing.

### 6.3 Post-incident checklist

- [ ] Service is UP and healthy in Prometheus
- [ ] `ServiceDown` alert is `Normal` in Grafana
- [ ] `resolved` notification received on webhook
- [ ] End-to-end POST /orders returns 201
- [ ] DLQ depth is 0
- [ ] Root cause identified and documented
- [ ] Corrective action taken (fix deployed or ticket created)

---

## Simulated incident — reference

This incident was simulated on 2026-06-18 to validate the runbook and observability stack.

**Trigger:** `docker compose stop order-service`

**Timeline:**

| Time | Event |
|---|---|
| T+0s | order-service stopped |
| T+15s | Prometheus scrape fails — target shows DOWN |
| T+60s | `ServiceDown` alert transitions from `Inactive` to `Firing` |
| T+90s | Grafana evaluates rule — `ServiceDown` confirmed Firing |
| T+90s | Webhook POST received: `"status":"firing"`, `"job":"order-service"` |
| T+120s | `docker compose start order-service` executed |
| T+180s | Service healthy, Prometheus target UP |
| T+240s | `ServiceDown` transitions to `Normal` |
| T+240s | Webhook POST received: `"status":"resolved"` |

**Root cause:** Manual stop for runbook validation — no corrective action required.

**Observations:**
- Detection latency: ~90 seconds from stop to firing notification (15s scrape interval + 60s `for` clause + evaluation cycle)
- Recovery latency: ~120 seconds from service start to resolved notification
- DLQ depth: 0 (no messages were in flight at time of stop)
- Loki logs showed clean shutdown, no error logs
- Tempo: no active traces at time of stop (no in-flight requests)