# ADR-004: PostgreSQL as Primary Datastore

## Status: Accepted

## Context

The backend services in this sandbox require a relational datastore for persistent storage.
A decision is needed on which database engine to use as the primary datastore.

Alternatives considered:
- PostgreSQL — open-source, production-grade relational database
- MySQL / MariaDB — widely used relational alternative
- H2 — in-memory database, often used in development and testing
- MongoDB — document-oriented NoSQL alternative

## Decision

Use **PostgreSQL** as the primary datastore for all services requiring persistent storage.

PostgreSQL will run as a Docker container locally (Phase 2) and as a managed RDS instance
on AWS (Phase 7). Integration tests will use Testcontainers with the official PostgreSQL image.
H2 will not be used — even in tests — to ensure test environments reflect production.

## Consequences

**Positive:**
- PostgreSQL is the de facto standard for open-source relational databases in cloud-native environments
- Full ACID compliance, strong consistency, and mature support for complex queries and transactions
- First-class support in Spring Data JPA / Hibernate
- Testcontainers provides a real PostgreSQL instance for integration tests — no mocking
- Direct mapping to AWS RDS PostgreSQL in Phase 7 — no engine switch required
- Widely used in Swiss and European enterprise environments

**Negative:**
- Requires Docker to be running for local development (no lightweight in-memory fallback)
- Slightly more setup overhead than H2 for simple cases

**Neutral:**
- Schema migrations will be managed via Flyway or Liquibase (decision deferred to Phase 1)
- Connection pooling will be handled by HikariCP (Spring Boot default)
