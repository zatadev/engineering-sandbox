# Engineering Sandbox

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

| Domain         | Technology                                      |
|----------------|-------------------------------------------------|
| Language       | Java 21                                         |
| Framework      | Spring Boot 3.5.x                               |
| Build          | Maven (multi-module monorepo)                   |
| Database       | PostgreSQL + JPA/Hibernate + Flyway             |
| Cache          | Redis                                           |
| Security       | Spring Security, OAuth2/OIDC, JWT, Keycloak     |
| Messaging      | RabbitMQ, Kafka                                 |
| Containers     | Docker, Docker Compose                          |
| Orchestration  | Kubernetes (minikube / kind)                    |
| CI/CD          | GitLab CI/CD                                    |
| Testing        | JUnit 5, Mockito, Testcontainers                |
| Observability  | Prometheus, Grafana, OpenTelemetry, Loki        |
| IaC            | Terraform                                       |
| Cloud          | AWS (ECS, EKS, RDS, S3, IAM, VPC)              |
| Secret Mgmt    | HashiCorp Vault                                 |

---

## Repository Structure

```text
engineering-sandbox/
├── services/
│   ├── user-service/          # Phase 1 — core REST service ✅
│   ├── order-service/         # Phase 4 — event producer
│   └── notification-service/  # Phase 4 — event consumer
├── infrastructure/
│   ├── docker/                # Dockerfiles
│   ├── docker-compose/        # Phase 2 — local environment
│   ├── kubernetes/            # Phase 6 — K8s manifests
│   └── terraform/             # Phase 7 — IaC
├── messaging/
│   ├── rabbitmq/              # Phase 4
│   └── kafka/                 # Phase 4
├── observability/
│   ├── prometheus/            # Phase 5
│   ├── grafana/               # Phase 5
│   ├── otel/                  # Phase 5 — OpenTelemetry
│   └── loki/                  # Phase 5 — log aggregation
├── ci-cd/
│   └── gitlab-ci/             # Phase 3
└── docs/
    ├── adr/                   # Architecture Decision Records
    ├── postman/               # Postman collections per service
    ├── runbooks/              # Operational runbooks
    └── architecture/          # Diagrams, system design notes
```

---

## Progression Plan

| Phase | Scope                        | Status         |
|-------|------------------------------|----------------|
| 0     | Repository Foundations       | ✅ Complete    |
| 1     | Java Service (user-service)  | ✅ Complete    |
| 2     | Docker Compose + Redis       | 🔜 Next        |
| 3     | GitLab CI/CD                 | ⬜ Pending     |
| 4     | Messaging (RabbitMQ + Kafka) | ⬜ Pending     |
| 5     | Observability                | ⬜ Pending     |
| 6     | Kubernetes                   | ⬜ Pending     |
| 7     | Terraform & AWS              | ⬜ Pending     |
| 8     | Security & Final Polish      | ⬜ Pending     |

---

## Phase 1 — user-service

Production-grade REST service built with Spring Boot 3.5 / Java 21.

**What's included:**
- REST CRUD API: `GET /api/v1/users`, `POST`, `PUT /{id}`, `DELETE /{id}`
- JWT authentication stub (POST `/api/v1/auth/login`)
- PostgreSQL + Flyway migrations
- Spring Actuator: `/health`, `/info`, `/metrics`, `/actuator/prometheus`
- Structured JSON logging with correlation IDs
- Unit tests (JUnit 5 + Mockito) and integration tests (Testcontainers)
- Multi-stage Dockerfile (JDK builder → JRE runtime, non-root user)
- Postman collection with 25 automated tests

**Run locally:**
```bash
# Start PostgreSQL
docker run -d --name postgres-dev \
  -e POSTGRES_DB=userdb -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16-alpine

# Build and start user-service
docker build -f services/user-service/Dockerfile -t user-service:latest .
docker run -d --name user-service-dev \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/userdb \
  -e SPRING_PROFILES_ACTIVE=dev \
  -p 8080:8080 user-service:latest
```

See [`docs/runbooks/user-service-local.md`](./docs/runbooks/user-service-local.md) for full instructions.

---

## Architectural Decisions

All significant technical decisions are documented as ADRs in [`/docs/adr/`](./docs/adr/).

| ADR     | Decision                                        |
|---------|-------------------------------------------------|
| ADR-001 | GitLab CI/CD over GitHub Actions                |
| ADR-002 | Monorepo structure                              |
| ADR-003 | Java 21 + Spring Boot 3                         |
| ADR-004 | PostgreSQL as primary datastore                 |
| ADR-005 | RabbitMQ before Kafka                           |
| ADR-006 | Redis for caching                               |
| ADR-007 | Keycloak as identity provider                   |
| ADR-008 | DTO separation and error handling strategy      |

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
- Each phase is tagged: `v0.1.0`, `v0.2.0`, etc.

---

## Navigation

- **Architecture decisions** → [`/docs/adr/`](./docs/adr/)
- **Postman collections** → [`/docs/postman/`](./docs/postman/)
- **Operational runbooks** → [`/docs/runbooks/`](./docs/runbooks/)
- **Architecture diagrams** → [`/docs/architecture/`](./docs/architecture/)
- **Contributing guidelines** → [`CONTRIBUTING.md`](./CONTRIBUTING.md)
