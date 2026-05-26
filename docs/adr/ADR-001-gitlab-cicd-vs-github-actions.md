# ADR-001: GitLab CI/CD over GitHub Actions

## Status: Accepted

## Context

This project needs a CI/CD pipeline covering build, test, quality, and deployment.
The sandbox targets Swiss and European enterprise environments — banking and finance
in particular — where GitLab runs on-premise or self-hosted and is often the only option.
Practicing on GitHub Actions here would miss the point.

Two options were considered:
- **Option A**: GitLab as primary repository and CI/CD platform
- **Option B**: GitHub as primary repository, mirrored to GitLab for pipeline execution

## Decision

GitLab.com is the primary repository and CI/CD platform. GitHub is a read-only push mirror
for recruiter visibility.

After every commit, GitLab pushes to GitHub automatically. The pipeline is defined in
`.gitlab-ci.yml` and stored under `ci-cd/gitlab-ci/`.

Option B was rejected: mirror sync latency would gate every pipeline trigger, GitLab's
registry, secrets, and environments work best when GitLab owns the repo, and there's no
active collaboration on GitHub anyway so branch protections there would do nothing.

## Consequences

**Positive:**
- Direct experience with GitLab CI/CD, the standard tool in Swiss/European enterprise
- Every push triggers immediately, no sync latency
- Full GitLab feature set natively: registry, variables, environments, security scanning
- GitHub stays visible to recruiters without affecting the dev workflow
- Pipeline syntax (`.gitlab-ci.yml`, stages, jobs, artifacts, runners) applies directly to target employers

**Negative:**
- GitHub is a passive mirror — contributors have to work on GitLab, not GitHub
- More initial setup than GitHub Actions would have been

**Neutral:**
- GitHub branch protections are off (GitLab owns the repo, protections live there)
- Core pipeline concepts transfer between GitLab CI and GitHub Actions well enough
