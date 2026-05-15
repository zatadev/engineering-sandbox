# ADR-001: GitLab CI/CD over GitHub Actions

## Status: Accepted

## Context

This project requires a CI/CD pipeline to automate build, test, quality, and deployment stages.
The repository is hosted on GitHub, which natively offers GitHub Actions.
GitLab CI/CD is an alternative that requires either mirroring the repository to GitLab
or running a self-hosted GitLab instance.

The primary goal of this sandbox is interview preparation and skills demonstration
for enterprise environments in Switzerland and Europe. The target market heavily uses
GitLab in on-premise and self-hosted configurations, particularly in banking and finance.

## Decision

Use **GitLab CI/CD** as the primary pipeline tool, with the GitHub repository mirrored to GitLab.

The pipeline will be defined in `.gitlab-ci.yml` and stored under `ci-cd/gitlab-ci/`.

## Consequences

**Positive:**
- Direct, hands-on experience with GitLab CI/CD — the dominant tool in Swiss/European enterprise environments
- Pipeline syntax (`.gitlab-ci.yml`, stages, jobs, artifacts, runners) is directly transferable to target employers
- Demonstrates awareness of real-world enterprise tooling choices beyond the GitHub ecosystem
- GitLab's built-in container registry simplifies the Docker image publishing step

**Negative:**
- Requires maintaining a mirror between GitHub (public repo) and GitLab (pipeline execution)
- Slightly more setup overhead compared to native GitHub Actions
- Two platforms to manage instead of one

**Neutral:**
- GitHub Actions remains available as a fallback if GitLab mirroring becomes impractical
- The pipeline concepts (stages, artifacts, caching, secrets) are largely transferable between tools
