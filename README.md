# Engineering Sandbox

[![Pipeline](https://gitlab.com/zatadev/engineering-sandbox/badges/develop/pipeline.svg)](https://gitlab.com/zatadev/engineering-sandbox/-/commits/develop)

A structured, public, production-grade repository built for hands-on mastery of
modern cloud-native engineering — and as a credible artifact for senior/lead technical interviews.

> Built by a Senior Java Backend Engineer (~8 years, banking/finance) targeting
> Tech Lead, Backend+DevOps, and Cloud-Native roles in Switzerland and European enterprise environments.

---

## What This Is

This is not a product, a SaaS, or a startup project.

It is an **engineering sandbox** — a deliberate, incremental journey through the full
cloud-native stack, built with production standards in mind:
clean commits, documented decisions, tested code, and observable systems.

---

## Stack

| Domain         | Technology                                           |
|----------------|------------------------------------------------------|
| Language       | Java 21                                              |
| Framework      | Spring Boot 3.5.x                                    |
| Build          | Maven (multi-module monorepo)                        |
| Database       | PostgreSQL + JPA/Hibernate + Flyway                  |
| Cache          | Redis                                                |
| Security       | Spring Security, OAuth2/OIDC, JWT, Keycloak          |
| Messaging      | RabbitMQ, Kafka                                      |
| Containers     | Docker, Docker Compose                               |
| Orchestration  | Kubernetes (minikube / kind)                         |
| CI/CD          | GitLab CI/CD                                         |
| Testing        | JUnit 5, Mockito, Testcontainers                     |
| Observability  | Prometheus, Grafana, OpenTelemetry, Loki, Tempo      |
| IaC            | Terraform                                            |
| Cloud          | AWS (ECS, EKS, RDS, S3, IAM, VPC)                   |
| Secret Mgmt    | HashiCorp Vault                                      |

---

## Repository Structure

```text
engineering-sandbox/
├── services/
│   ├── common/                # Phase 4 — shared infrastructure ✅
│   ├── user-service/          # Phase 1 — core REST service ✅
│   ├── order-service/         # Phase 4 — event producer ✅
│   └── notification-service/  # Phase 4 — event consumer ✅
├── infrastructure/
│   ├── docker/                # Dockerfiles
│   ├── docker-compose/        # Phase 2 — local environment ✅
│   ├── kubernetes/            # Phase 6 — K8s manifests
│   └── terraform/             # Phase 7 — IaC
├── messaging/
│   ├── rabbitmq/              # Phase 4 ✅
│   └── kafka/                 # Phase 4 ✅
├── observability/
│   ├── prometheus/            # Phase 5 ✅ — scraping + alert rules
│   ├── grafana/               # Phase 5 ✅ — dashboards + alerting
│   ├── otel/                  # Phase 5 ✅ — Tempo config
│   └── loki/                  # Phase 5 ✅ — log aggregation
├── ci-cd/
│   └── gitlab-ci/             # Phase 3 ✅
└── docs/
    ├── adr/                   # Architecture Decision Records
    ├── postman/               # Postman collections per service
    ├── runbooks/              # Operational runbooks
    └── architecture/          # Diagrams, system design notes
```

---

## Running Locally

### Prerequisites
- Docker Desktop installed and running
- Git

### Setup

```bash
# 1. Clone the repository
git clone git@github.com:zatadev/engineering-sandbox.git
cd engineering-sandbox

# 2. Configure environment variables
cp infrastructure/docker-compose/.env.example \
   infrastructure/docker-compose/.env
# Edit .env with your local values

# 3. Start the full stack
docker compose -f infrastructure/docker-compose/docker-compose.yml up --build
```

### Verify everything is running

```bash
docker ps
# Should show all services as healthy
```

Then hit the health endpoints:
```bash
curl http://localhost:8080/actuator/health  # user-service
curl http://localhost:8081/actuator/health  # order-service
curl http://localhost:8082/actuator/health  # notification-service
```

### Services

| Service              | URL                    | Notes                        |
|----------------------|------------------------|------------------------------|
| user-service         | http://localhost:8080  | REST API                     |
| order-service        | http://localhost:8081  | REST API + event producer    |
| notification-service | http://localhost:8082  | Event consumer (stateless)   |
| postgres             | localhost:5432         | userdb + orderdb             |
| redis                | localhost:6379         | Caching + idempotency store  |
| rabbitmq             | http://localhost:15672 | Management UI (guest/guest)  |
| kafka                | localhost:9092         | KRaft mode, no Zookeeper     |
| prometheus           | http://localhost:9090  | Metrics + alert rules        |
| grafana              | http://localhost:3000  | Dashboards + alerting (admin/admin) |
| loki                 | http://localhost:3100  | Log aggregation              |
| tempo                | http://localhost:3200  | Distributed traces           |

### Logs

```bash
# All services
docker compose -f infrastructure/docker-compose/docker-compose.yml logs -f

# Single service
docker compose -f infrastructure/docker-compose/docker-compose.yml logs -f order-service
```

### Metrics & Observability

Spring Actuator endpoints available on each service:

| Endpoint               | Description       |
|------------------------|-------------------|
| `/actuator/health`     | Health status     |
| `/actuator/info`       | Build info        |
| `/actuator/metrics`    | Raw metrics       |
| `/actuator/prometheus` | Prometheus scrape |

Grafana dashboards available at `http://localhost:3000`:
- **JVM Metrics** — heap, GC, threads per service
- **HTTP Metrics** — request rate, error rate, latency p50/p95/p99
- **Business Metrics** — orders created, notifications sent, DLQ depth
- **Log Explorer** — structured logs with trace correlation

### Full runbook

See [`docs/runbooks/docker-compose-local.md`](docs/runbooks/docker-compose-local.md)
for advanced usage: resetting the database, inspecting the Redis cache, troubleshooting.

For incident investigation, see [`docs/runbooks/incident-service-down.md`](docs/runbooks/incident-service-down.md).

---

## Progression Plan

| Phase | Scope                        | Status          |
|-------|------------------------------|-----------------|
| 0     | Repository Foundations       | ✅ Complete     |
| 1     | Java Service (user-service)  | ✅ Complete     |
| 2     | Docker Compose + Redis       | ✅ Complete     |
| 3     | GitLab CI/CD                 | ✅ Complete     |
| 4     | Messaging (RabbitMQ + Kafka) | ✅ Complete     |
| 5     | Observability                | ✅ Complete     |
| 6     | Kubernetes                   | ✅ Complete     |
| 7     | Terraform & AWS              | ⬜ In progress  |
| 8     | Security & Final Polish      | ⬜ Pending      |

---

## Phase 1 — user-service

Production-grade REST service built with Spring Boot 3.5 / Java 21.

**What's included:**
- REST CRUD API: `GET /api/v1/users`, `POST`, `PUT /{id}`, `DELETE /{id}`
- JWT authentication stub (POST `/api/v1/auth/login`)
- PostgreSQL + Flyway migrations
- Redis caching (`@Cacheable` on reads, `@CacheEvict` on writes)
- Spring Actuator: `/health`, `/info`, `/metrics`, `/actuator/prometheus`
- Structured JSON logging with correlation IDs
- Unit tests (JUnit 5 + Mockito) and integration tests (Testcontainers)
- Multi-stage Dockerfile (JDK builder → JRE runtime, non-root user)
- Postman collection with automated tests

See [`docs/runbooks/user-service-local.md`](./docs/runbooks/user-service-local.md)
for the standalone `docker run` approach (Phase 1 reference).

---

## Phase 4 — Messaging (order-service + notification-service)

Event-driven architecture with RabbitMQ and Kafka running in parallel.

**What's included:**

**order-service** (port 8081):
- REST CRUD API: `GET /api/v1/orders`, `POST`, `DELETE /{id}`
- PostgreSQL + Flyway migrations (UUID primary keys)
- Publishes `OrderCreatedEvent` to RabbitMQ (`sandbox.orders` TopicExchange)
- Publishes `OrderCreatedEvent` to Kafka (`order.created` topic, 3 partitions)
- Correlation ID injected into AMQP and Kafka record headers

**notification-service** (port 8082):
- Stateless — no database
- Consumes `OrderCreatedEvent` from RabbitMQ and Kafka in parallel
- Idempotent consumer via Redis SETNX (24h TTL)
- Dead-letter queue with retry (3 attempts, exponential backoff: 1s → 2s → 4s)
- DLQ message count exposed as Micrometer gauge

**services/common**:
- Shared infrastructure module auto-configured via Spring Boot
- `CorrelationIdFilter`, `MetricsConfig`, `BaseGlobalExceptionHandler`
- `OrderCreatedEvent` shared DTO, unified `logback-spring.xml`

---

## Phase 5 — Observability (LGTM Stack)

Full observability stack: metrics, logs, traces, and alerting.

**Metrics — Prometheus + Micrometer**
- Prometheus scrapes all 3 services every 15s
- Custom business counters: orders registered, notifications sent (by source: rabbitmq/kafka)
- DLQ depth gauge, JVM heap, GC, thread metrics

**Visualization — Grafana**
- 4 dashboards: JVM Metrics, HTTP Metrics, Business Metrics, Log Explorer
- Auto-provisioned datasources and dashboards — no manual configuration required
- Alerting rules provisioned as code: ServiceDown, HighErrorRate, HighHeapMemory, DLQMessagesPresent
- Webhook notifications with firing and resolved lifecycle

**Distributed Tracing — OpenTelemetry + Tempo**
- Micrometer Tracing bridge (OTel) — no Java agent required
- End-to-end traces: HTTP → RabbitMQ publish → Kafka publish → consumers
- One-click navigation from log line to trace (Loki derivedFields → Tempo)
- Reverse navigation from trace span to logs (Tempo tracesToLogsV2 → Loki)

**Log Aggregation — Loki + Promtail**
- Promtail auto-discovers containers via Docker socket
- Structured JSON logs with `traceId`, `spanId`, `correlationId` in every line
- LogQL queries for error investigation by service and level

**Documentation**
- Incident runbook: [`docs/runbooks/incident-service-down.md`](docs/runbooks/incident-service-down.md)
- ADR-012: Observability strategy

---

## Phase 6 — Kubernetes (user-service)

Full Kubernetes deployment pattern, built on **user-service as the reference service**
(replication to the other services tracked in ZAT-193).

**Cluster**
- kind, multi-node (1 control-plane + 2 workers) — kind vs minikube rationale in ADR-013
- metrics-server vendored (patched for kind)

**Workload**
- Deployment (2 replicas) + ClusterIP Service + ConfigMap + Secret
- Liveness / readiness / startup probes (Spring Boot Actuator)
- Resource requests and limits (Burstable QoS)

**Ingress — ingress-nginx**
- Path routing: `/api/v1/users` → user-service
- Controller pinned to the ingress-ready node (multi-node fix)
- ingress-nginx retired (March 2026) → Gateway API migration planned (ZAT-190/191/192)

**Autoscaling — HPA**
- min 2 / max 5, target CPU 70%
- Load-tested with `hey`: scale-up 2→5, graduated scale-down to 2

**Deployment lifecycle**
- Rolling update + rollback demonstrated (zero-downtime, readiness-gated)

**Documentation**
- ADR-013: Kubernetes strategy
- Runbooks: `hpa-user-service`, `rolling-deployment-user-service`

---

## Architectural Decisions

All significant technical decisions are documented as ADRs in [`/docs/adr/`](./docs/adr/).

| ADR     | Decision                                             |
|---------|------------------------------------------------------|
| ADR-001 | GitLab CI/CD over GitHub Actions                     |
| ADR-002 | Monorepo structure                                   |
| ADR-003 | Java 21 + Spring Boot 3                              |
| ADR-004 | PostgreSQL as primary datastore                      |
| ADR-005 | RabbitMQ before Kafka                                |
| ADR-006 | Redis for caching                                    |
| ADR-007 | Keycloak as identity provider                        |
| ADR-008 | DTO separation and error handling strategy           |
| ADR-009 | Secrets management strategy                          |
| ADR-010 | CI/CD strategy and pipeline design                   |
| ADR-011 | RabbitMQ vs Kafka — when to use which                |
| ADR-012 | Observability strategy — LGTM stack, OTel, correlation |
| ADR-013 | Kubernetes strategy — kind, namespaces, config/secrets, resources |

---

## Conventions

### Commit Messages

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

```
feat:     new feature or capability
fix:      bug fix
chore:    tooling, config, dependencies
docs:     documentation, ADRs, README
ci:       CI/CD pipeline changes
infra:    infrastructure, Docker, Kubernetes, Terraform
obs:      observability — metrics, tracing, logging
refactor: code restructuring without behavior change
test:     adding or updating tests
```

### Branch Strategy

```
main        → stable, always deployable — never commit directly
develop     → integration branch — never commit directly
feature/*   → one feature per branch
infra/*     → infrastructure changes
ci/*        → pipeline changes
obs/*       → observability work
docs/*      → documentation only
```

### Pull Requests

- Feature branches → `develop`: **Squash and merge** (one commit per PR)
- `develop` → `main`: **Merge commit** at phase completion
- Each phase is tagged: `v0.1.0`, `v0.2.0`, ..., `v0.7.0`

---

## Navigation

- **Architecture decisions** → [`/docs/adr/`](./docs/adr/)
- **Postman collections** → [`/docs/postman/`](./docs/postman/)
- **Operational runbooks** → [`/docs/runbooks/`](./docs/runbooks/)
- **Architecture diagrams** → [`/docs/architecture/`](./docs/architecture/)
- **Contributing guidelines** → [`CONTRIBUTING.md`](./CONTRIBUTING.md)