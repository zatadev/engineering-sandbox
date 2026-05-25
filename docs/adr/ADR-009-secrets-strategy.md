# ADR-009: Secrets Management Strategy

## Status: Accepted

## Context

The engineering sandbox handles sensitive configuration values across multiple
environments: local development, CI/CD pipelines, and production. These include
database credentials, JWT signing keys, and service passwords.

Secrets must never be committed to the Git repository — not even in private repos.
Reasons:
- Git history is permanent: a secret committed and later removed is still
  accessible in the history
- Repository access is often broader than production access
- Leaked secrets in public repos are scanned and exploited within minutes
  by automated bots
- Compliance requirements (SOC2, ISO 27001) explicitly prohibit secrets in source code

## Decision

We adopt a three-tier secrets strategy aligned with environment maturity:

### Tier 1 — Local Development

Secrets are stored in `infrastructure/docker-compose/.env`, which is:
- Never committed to Git (enforced via `.gitignore`)
- Documented via `infrastructure/docker-compose/.env.example` (committed,
  contains only placeholder values)
- Set up manually by each developer on first clone

This approach is intentionally simple — local development does not require
rotation, auditing, or access control.

### Tier 2 — CI/CD (GitLab CI/CD)

Secrets are injected as GitLab CI/CD variables (Settings → CI/CD → Variables):
- Marked as `Protected` (only available on protected branches)
- Marked as `Masked` (never visible in job logs)
- Never hardcoded in `.gitlab-ci.yml`

In pipeline jobs, secrets are accessed as environment variables:
```yaml
deploy:
  script:
    - echo $DB_PASSWORD  # injected by GitLab, never in the file
```

### Tier 3 — Production (Phase 8)

Secrets will be managed by HashiCorp Vault:
- Dynamic secrets with short TTLs (database credentials rotated automatically)
- Audit log of every secret access
- Fine-grained policies per service
- Spring Boot integration via `spring-cloud-vault`

This tier is out of scope for Phase 2 and will be implemented in Phase 8.

## Redis Authentication

Redis runs without authentication in local development. This is intentional —
Redis is not exposed outside the Docker network, and local dev does not require
the same security posture as production.

Redis authentication (requirepass, TLS) will be configured in Phase 8 alongside
the full security review.

## Consequences

**Positive:**
- No secrets ever exposed in Git history
- Clear escalation path from local → CI/CD → production
- Each tier uses the simplest approach appropriate for its context
- Production-ready strategy documented before it is needed

**Negative:**
- New developers must manually create their `.env` file on first clone
  (mitigated by `.env.example` and runbook documentation)
- Local secrets are not rotated or audited (acceptable for dev environment)

## References
- `infrastructure/docker-compose/.env.example`
- `docs/runbooks/docker-compose-local.md`
- Phase 8: HashiCorp Vault integration