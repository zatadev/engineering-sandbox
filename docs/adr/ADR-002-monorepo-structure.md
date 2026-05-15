# ADR-002: Monorepo Structure

## Status: Accepted

## Context

This sandbox covers multiple services (user-service, order-service, notification-service),
multiple infrastructure concerns (Docker, Kubernetes, Terraform), observability tooling,
and shared documentation. A decision is needed on whether to organize these as a monorepo
(single repository, all components) or as polyrepo (one repository per service or concern).

The project is an engineering sandbox, not a production system with independent deployment
cadences or separate teams per service. The priority is learning, coherence, and navigability.

## Decision

Use a **monorepo** structure with a clear top-level directory separation by concern:
`services/`, `infrastructure/`, `messaging/`, `observability/`, `ci-cd/`, `docs/`.

Each service under `services/` is self-contained with its own build file, Dockerfile, and tests.

## Consequences

**Positive:**
- Single clone, single context — easy to navigate and present to recruiters
- Cross-cutting changes (observability config, shared conventions) are visible and traceable in one place
- ADRs, runbooks, and architecture diagrams live alongside the code they document
- Simpler CI/CD setup — one pipeline definition covers all components
- Easier to demonstrate architectural coherence across phases

**Negative:**
- Does not reflect the polyrepo model used by some large organizations
- As the project grows, build times may increase if not scoped correctly
- Requires discipline to keep service boundaries clean without enforced module isolation

**Neutral:**
- Each service remains independently buildable and deployable
- The monorepo approach is common in mid-sized engineering teams and well understood in interviews
