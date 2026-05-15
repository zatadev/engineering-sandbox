# Contributing

This document describes the conventions and standards used in this repository.
It serves as the reference for anyone working on or reviewing this project.

---

## Prerequisites

Make sure the following tools are installed before working on this project:

| Tool          | Version     | Purpose                        |
|---------------|-------------|--------------------------------|
| JDK           | 21          | Java runtime and compiler      |
| Docker        | Latest      | Containerization               |
| Docker Compose| Latest      | Local multi-service environment|
| Git           | Latest      | Version control                |
| Maven or Gradle| Latest     | Build tool                     |

---

## Getting Started

```bash
# Clone the repository
git clone git@github.com:zatadev/engineering-sandbox.git
cd engineering-sandbox

# Each service is self-contained under services/
cd services/user-service

# Build (Maven example)
./mvnw clean install

# Run the full local stack (from Phase 2 onward)
docker compose -f infrastructure/docker-compose/docker-compose.yml up
```

---

## Branch Strategy

```
main        → stable, always deployable — no direct push
develop     → integration branch — target of all PRs
feature/*   → one feature per branch
infra/*     → infrastructure changes
ci/*        → CI/CD pipeline changes
obs/*       → observability work
docs/*      → documentation only
```

**Rules:**
- Never push directly to `main`
- All work goes through a PR into `develop`
- `main` is updated only at phase completion via PR from `develop`
- One concern per branch — keep branches short-lived

---

## Commit Messages

This project follows [Conventional Commits](https://www.conventionalcommits.org/).

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

**Examples:**

```
feat: add user registration endpoint
fix: correct pagination offset in user listing
chore: upgrade Spring Boot to 3.3.0
docs: add ADR-003 Java 21 decision
infra: add multi-stage Dockerfile for user-service
test: add Testcontainers integration test for user repository
```

**Rules:**
- Use the imperative mood: "add", not "added" or "adds"
- Keep the subject line under 72 characters
- No period at the end of the subject line
- Reference the phase and component in the PR description, not the commit

---

## Pull Requests

Every PR must include:

- A clear title following the commit convention
- A description of **what** was done and **why**
- Reference to the phase and component (e.g. `Phase 1 — user-service: add health checks`)
- Passing tests before requesting review

PR merge strategy:
- **Squash merge** into `develop`
- **Merge commit** from `develop` into `main` at phase completion

---

## Code Standards

### Java

- Java 21 — use modern features where appropriate (records, sealed classes, pattern matching)
- Follow standard Java naming conventions
- No unused imports, no commented-out code in commits
- All public methods must have Javadoc if part of a public API

### Testing

- Unit tests with JUnit 5 for business logic
- Integration tests with Testcontainers for database and messaging
- Tests must pass locally before pushing
- Aim for meaningful coverage — test behavior, not implementation

### Configuration

- No hardcoded credentials or secrets anywhere in the codebase
- Use environment variables or `application.yml` profiles
- Secrets in production are managed via Vault (Phase 8)

---

## Architecture Decision Records

Every significant technical decision must be documented as an ADR in [`/docs/adr/`](./docs/adr/).

Use this format:

```markdown
# ADR-XXX: Title
## Status: Proposed | Accepted | Deprecated
## Context
## Decision
## Consequences
```

An ADR is required when:
- Choosing a technology or library over alternatives
- Defining a cross-cutting architectural pattern
- Making a trade-off with non-obvious consequences

---

## Documentation

- Keep `README.md` up to date when the stack or structure changes
- Update the progression plan table when a phase is completed
- Runbooks go in [`/docs/runbooks/`](./docs/runbooks/)
- Architecture diagrams go in [`/docs/architecture/`](./docs/architecture/)
