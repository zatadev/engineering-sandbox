# ADR-010: CI/CD strategy and pipeline design

## Status: Accepted

## Context

This sandbox is built to production-grade standards for Swiss and European banking
environments. The CI/CD pipeline needs to reflect that:

- Automated build, test, quality checks, and Docker image publication
- Quality gates that block merges on failure
- Semantic versioning baked into the pipeline
- Clear branch protection and trigger policies
- A documented rollback path

## Decision

### Pipeline stages

The pipeline is defined in `.gitlab-ci.yml` with four stages in order:

| Stage | Trigger | Purpose |
|---|---|---|
| `build` | All branches + MR | Compile and package the JAR, fail fast on compilation errors |
| `test` | All branches + MR | Unit + integration tests via Testcontainers, JUnit reports |
| `quality` | MR + `develop` + `main` | SonarCloud static analysis, coverage via JaCoCo |
| `docker` | MR (build only) + `develop` + `main` | Build and push image to GitLab Container Registry |

Stage order is intentional. `build` fails fast before running expensive tests. `test`
runs before quality so JaCoCo reports are ready for SonarCloud. `docker` runs last —
you only publish an image that's been built, tested, and quality-checked.

Selective triggers follow the same logic. `quality` skips feature branches — the cost
isn't worth it for every push; full analysis runs on MR (pre-merge gate) and on
`develop`/`main` (post-merge). `docker` builds on MR without pushing, to catch
Dockerfile issues early. Registry pushes only happen on `develop` and `main`.

### Quality gates

SonarCloud is the quality gate provider (free for public repositories). JaCoCo
generates XML coverage reports that SonarCloud consumes.

The pipeline fails if SonarCloud analysis fails. Gate thresholds are configured in
SonarCloud and apply to new code only (PR analysis mode).

### Semantic versioning

This project follows [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH).
All versions are `0.x.x` during active sandbox development (pre-Phase 8).

Docker image tags:

| Context | Tag |
|---|---|
| Feature branch MR | `mr-{SHA}` (not pushed) |
| `develop` push | `develop-{SHA}` |
| `main` push | `{git_tag}` + `latest` |

Phase completion tags follow `v0.N.0` where N is the phase number. Tags are created
manually after merging `develop` → `main`.

### Branch protection and pipeline triggers

```
main     → protected — no direct push, MR from develop only
develop  → protected — no direct push, all feature work via MR
```

| Event | build | test | quality | docker |
|---|---|---|---|---|
| Push to feature branch | ✅ | ✅ | ❌ | ❌ |
| MR opened/updated | ✅ | ✅ | ✅ | ✅ (no push) |
| Merge to develop | ✅ | ✅ | ✅ | ✅ (push) |
| Merge to main | ✅ | ✅ | ✅ | ✅ (push + latest) |

### Rollback strategy

See [`/docs/runbooks/rollback.md`](../runbooks/rollback.md) for the full procedure.

Two options:

**Option A — Git tag rollback (recommended):** Check out the last stable tag, create a
new patch tag, and let the pipeline rebuild and republish. Slower, but leaves a full
audit trail.

**Option B — Docker image re-tag (faster):** Re-tag a previously published image as
`latest` and push directly to the registry. Bypasses the pipeline — use only when
rebuild time is unacceptable.

## Consequences

Quality gate on every MR prevents regressions from reaching `develop`. Docker images
are only published after passing build, test, and quality. Semantic versioning makes
every deployed version traceable.

The downsides: `quality` adds ~1 minute to every MR pipeline. We depend on
SonarCloud — if it's down, the pipeline fails. Manual tagging at phase completion
isn't ideal but acceptable for this project's pace.

Pipeline minutes stay within GitLab.com free tier. SonarCloud free tier covers this
project's scope.
