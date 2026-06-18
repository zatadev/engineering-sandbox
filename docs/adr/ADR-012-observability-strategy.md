# ADR-012: Observability strategy

## Status: Accepted

## Context

The engineering-sandbox is a distributed system with three services (user-service, order-service, notification-service) communicating via HTTP, RabbitMQ, and Kafka. Debugging failures in a distributed system without proper observability is impractical: a single user request can span multiple services, and a failure in one service can manifest as an error in another.

Three categories of data are needed to understand system behavior in production:

- **Metrics** — aggregated numeric measurements over time (request rates, error rates, latency percentiles, JVM heap usage). Optimized for alerting and dashboards.
- **Logs** — discrete events emitted by services with structured context (correlationId, traceId, level, message). Optimized for root cause investigation.
- **Traces** — end-to-end records of a request as it flows through multiple services and operations. Optimized for latency analysis and understanding distributed causality.

Each pillar answers a different question:
- Metrics: *is something wrong?*
- Logs: *what happened?*
- Traces: *where did it happen and why?*

Without all three, debugging a production incident requires guesswork. With all three correlated, the path from alert to root cause is deterministic.

## Decision

### Three-pillar observability stack

The following stack has been adopted for Phase 5:

| Pillar | Technology | Role |
|---|---|---|
| Metrics | Prometheus + Micrometer | Scraping, storage, alerting rules |
| Visualization | Grafana | Unified UI for all three pillars |
| Logs | Loki + Promtail | Log aggregation and querying |
| Traces | Tempo | Distributed trace storage and querying |
| Instrumentation | OpenTelemetry (via Micrometer Tracing) | Vendor-neutral trace generation and propagation |

### Why OpenTelemetry

OpenTelemetry (OTel) is the CNCF standard for observability instrumentation. It was chosen over vendor-specific SDKs (Datadog agent, New Relic SDK, Dynatrace):

- OTel separates instrumentation from the backend. The same code exports to Tempo today and to Jaeger, Datadog, or Honeycomb tomorrow without touching service code. In a banking/enterprise context, backend tooling changes with contracts; instrumentation must not.
- Spring Boot 3.x ships with first-class OTel support via Micrometer Tracing. The `micrometer-tracing-bridge-otel` bridge allows using Spring's native `@Timed` and `@Counted` annotations while exporting traces in OTel format. No separate agent required.
- OTel propagates trace context (W3C `traceparent` header) automatically across HTTP calls. For messaging (RabbitMQ, Kafka), propagation requires `setObservationEnabled(true)` but is handled by the framework, not application code.
- OTel has replaced OpenTracing and OpenCensus as the unified standard. Vendor agents increasingly implement OTel under the hood. Skills are directly transferable across stacks.

### Why Prometheus for metrics

Prometheus uses a **pull model**: it scrapes `/actuator/prometheus` endpoints on each service at a configurable interval (15s in this sandbox). This was chosen over a push model (StatsD, InfluxDB) because:

- Services do not need to know where Prometheus is — they expose metrics, Prometheus discovers and scrapes them
- Failed scrapes are immediately visible as target `DOWN` — the monitoring system itself surfaces dependency failures
- PromQL is the industry-standard query language for time-series metrics, directly usable in Grafana panels and alert rules

Micrometer acts as the instrumentation facade in Java: `@Timed`, `@Counted`, and manual `MeterRegistry` usage emit metrics that Micrometer converts to Prometheus format automatically.

### Why Loki for logs

Loki was chosen over the ELK stack (Elasticsearch + Logstash + Kibana):

- Loki indexes only labels (service, level, container), not log content. This cuts storage and CPU use compared to Elasticsearch full-text indexing. Log content is queried via LogQL after label-based filtering narrows the result set.
- Loki is developed by Grafana Labs and integrates natively into Grafana — the same UI used for metrics and traces. No separate Kibana instance required.
- A single Loki container with local filesystem storage is sufficient for this sandbox. Elasticsearch requires JVM tuning, index management, and significantly more resources.

Promtail is used as the log collector. It discovers service containers via the Docker socket (`docker_sd_configs`), reads their stdout logs, and parses the JSON structured output produced by logstash-logback-encoder. Labels (`service`, `level`) are extracted from the JSON and promoted to Loki index labels for efficient querying.

### Why Tempo for traces

Tempo was chosen over Jaeger because:

- Native Grafana integration — traces are visualized in the same Grafana instance used for metrics and logs
- OTLP support out of the box — no translation layer needed between OTel SDK and the backend
- Simpler operational model for a sandbox — Tempo requires less configuration than a full Jaeger deployment

Services export traces via OTLP HTTP to Tempo on port 4318. Tempo stores traces locally with a 1-hour retention (sandbox) and exposes a query API on port 3200 that Grafana uses as a datasource.

### Correlation between logs and traces

The three pillars are correlated through shared identifiers.

#### traceId in logs

Micrometer Tracing automatically populates the SLF4J MDC with `traceId` and `spanId` for the duration of each traced operation. `logback-spring.xml` (in the `common` module) includes `%mdc{traceId}` and `%mdc{spanId}` in the log pattern, and `LoggingEventCompositeJsonEncoder` includes all MDC fields in the JSON output. Every log line emitted during a traced request carries the same `traceId` as the corresponding Tempo trace.

#### Grafana Loki derivedFields

The Loki datasource is configured with a `derivedField` that extracts `traceId` from log lines using a regex and renders it as a clickable link to Tempo:

```yaml
derivedFields:
  - name: TraceID
    matcherRegex: '"traceId":"([a-f0-9]+)"'
    url: "${__value.raw}"
    datasourceUid: tempo
```

This enables one-click navigation from a log line to the corresponding distributed trace.

#### Tempo tracesToLogsV2

The Tempo datasource is configured with `tracesToLogsV2` pointing to Loki, enabling the reverse navigation: from a trace span to the logs emitted during that span's time window.

#### correlationId across async boundaries

In addition to OTel traceId, a `correlationId` (UUID) is injected at the HTTP gateway by `CorrelationIdFilter` and propagated via `X-Correlation-ID` headers in RabbitMQ and Kafka message headers. This predates OTel instrumentation (Phase 4) and provides an additional correlation key that survives async boundaries regardless of trace context propagation.

## Consequences

**Positive:**
- Single Grafana UI for metrics, logs, and traces — no context switching between tools during incident investigation
- Vendor-neutral instrumentation — backend can be swapped without touching service code
- Log-to-trace and trace-to-log navigation reduces mean time to root cause
- Alert rules committed as code (`sandbox-alerts.yml`) — observable as reproducible from `docker compose up`
- Standard industry stack (LGTM) directly applicable to enterprise environments

**Negative / trade-offs:**
- No Alertmanager — Grafana Alerting handles notification routing. Alertmanager would add deduplication, inhibition rules, and more flexible routing (e.g. critical → PagerDuty, warning → Slack). Acceptable for this sandbox; should be added before production use.
- 100% trace sampling — appropriate for development. Production requires probabilistic sampling (1–10%) and tail-based sampling for error cases to control cost and latency.
- Local storage for Loki and Tempo — 1h retention for traces, no long-term metrics storage (Thanos/Mimir). Acceptable for a local sandbox; production requires object storage (S3) and longer retention.
- Promtail reads from Docker socket — in Kubernetes (Phase 6), this will be replaced by a DaemonSet-based log collector (Promtail or Grafana Alloy).
