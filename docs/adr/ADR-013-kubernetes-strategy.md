# ADR-013: Kubernetes Strategy

**Date:** 2026-06-21
**Status:** Accepted

## Context

The application (currently `user-service`, with `order-service` and others to follow) must run on Kubernetes for local development, CI, and as a stepping stone to cloud deployment (Phase 7, EKS). This ADR records the foundational Kubernetes decisions for the sandbox:

- the local cluster tooling,
- the namespace organisation,
- the handling of configuration versus secrets,
- the resource requests/limits strategy.

The Ingress controller choice (ingress-nginx) and its planned migration to Gateway API are recorded separately in **ADR-014**.

## Decision

### 1. Local cluster: kind (over minikube)

We use **kind** (Kubernetes IN Docker) for the local and CI clusters, configured as **1 control-plane + 2 workers**.

- Nodes run as Docker containers, so cluster create/destroy is near-instant and a multi-node topology is trivial. Multi-node lets us exercise scheduling, pod distribution, rolling updates and autoscaling realistically — a single node hides these behaviours.
- kind is the tooling Kubernetes uses for its own CI: scriptable, reproducible, lightweight, and a natural fit for our CI/CD pipeline.
- Local Docker images are injected with `kind load docker-image`, so no image registry is required for the inner development loop.

We accept that kind is less "batteries included" than minikube: Ingress (`extraPortMappings` + an `ingress-ready` node) and metrics-server (`--kubelet-insecure-tls` for kind's self-signed kubelet certs) require manual setup. We mitigate this by **vendoring those manifests into the repo** and documenting the setup in runbooks, so the cluster remains reproducible.

### 2. Namespaces: separation by concern

- `sandbox` — application services (`user-service`, future `order-service`, …).
- `sandbox-infra` — stateful backing infrastructure (PostgreSQL, Redis).
- Third-party controllers live in their own namespaces (`ingress-nginx`; metrics-server in `kube-system`).

We deliberately avoid a namespace-per-service (unnecessary overhead at this scope) and a single flat namespace (no isolation). Concern-based grouping gives clean RBAC and resource-quota boundaries while staying proportionate to the project size.

### 3. Configuration: ConfigMap for config, Secret for credentials

- Non-sensitive configuration (Spring profile, service URLs, ports, feature flags) → `ConfigMap` (`user-service-config`), injected via `envFrom`.
- Credentials (database password, JWT secret) → `Secret` (`user-service-secret`), injected via `secretKeyRef`.

We explicitly acknowledge that native Kubernetes Secrets are **base64-encoded, not encrypted at rest** by default, and that secret manifests must never be committed to git in plaintext. For the sandbox this is accepted. The production path (encryption at rest + an external secrets manager such as Vault, or a GitOps-friendly approach like Sealed Secrets / SOPS) is planned for **Phase 8** and tracked as tech debt.

### 4. Resource management: requests AND limits on every container

Every container declares both requests and limits. Current `user-service` values:

| | CPU | Memory |
|---|---|---|
| requests | 100m | 256Mi |
| limits | 500m | 512Mi |

Rationale:

- **requests** are reserved for scheduling and are the **denominator for HPA CPU-utilisation** (the HPA target of 70% is 70% of the request).
- **limits** cap a runaway pod to protect the node from noisy-neighbour effects.
- With `requests < limits`, pods are **Burstable** QoS.

Honest caveats, observed during the HPA work (ADR-related runbook `hpa-user-service`):

- The `cpu: 100m` request is **demo-tuned** to make autoscaling observable in the sandbox. Production values must come from profiling, not from a desire to trigger the HPA.
- The `memory: 256Mi` request is low versus the observed steady-state of ~355Mi (138% of request); it should be raised to ~384Mi.
- CPU limits cause throttling. Under load, new pods throttled at their limit are slow to become `Ready` (observed during HPA scale-up). The trade-off — node protection versus startup latency — is accepted for the sandbox.

## Consequences

**Positive**

- Fast, reproducible, multi-node local and CI clusters; the setup mirrors real production concerns (scheduling, rolling updates, autoscaling).
- Clear separation of application/infrastructure and of configuration/credentials; resource discipline enables the HPA and protects nodes.
- The manual kind setup (Ingress, metrics-server) is vendored and documented, so the environment is reproducible from the repo.

**Negative / accepted debt**

- kind requires manual Ingress/metrics-server setup, whereas minikube offers these as one-command addons.
- Native Secrets are not encrypted at rest; the secrets-in-repo strategy is unresolved until Phase 8 (Vault).
- Some resource values are demo-tuned and need profiling-based revision before any production use.

## Alternatives Considered

- **minikube** — richer addons (Ingress, metrics-server in one command), but heavier, single-node by default, and less aligned with CI. Rejected in favour of kind's CI fit and multi-node realism.
- **k3d / Docker Desktop Kubernetes** — k3d is a viable, lightweight k3s-based alternative; Docker Desktop's cluster is single-node and less controllable. kind preferred for its ubiquity in Kubernetes CI.
- **Cloud cluster (EKS) from the start** — too slow and costly for the inner development loop; reserved for Phase 7.
- **Single namespace** — no isolation; rejected. **Namespace-per-service** — over-engineered for current scope; rejected.
- **All-ConfigMap or all-Secret** — rejected: the former exposes credentials, the latter makes ordinary config needlessly opaque.
- **External secrets manager (Vault) from day one** — overkill for the sandbox; planned for Phase 8.
- **No CPU/memory limits** — loses node protection against runaway pods; rejected despite the startup-throttling trade-off.

---

*Related:* ADR-014 (Ingress → Gateway API migration). *Runbooks:* `hpa-user-service`, `rolling-deployment-user-service`.