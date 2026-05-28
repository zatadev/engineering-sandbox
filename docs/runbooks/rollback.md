# Rollback Runbook

**Last updated:** 2026-05  
**Service:** user-service  
**Author:** zatadev

> This runbook is written for 3am incidents.
> Follow the steps in order. Do not improvise.

---

## When to trigger a rollback

Trigger a rollback immediately if **any** of the following is true:

- `/actuator/health` returns non-200 for more than 2 minutes
- Error rate > 5% on any endpoint (check Grafana)
- A critical bug is confirmed in production
- The deployment pipeline itself failed mid-deploy

Do **not** rollback for:
- Warnings in logs (not errors)
- A single failed request
- Performance degradation under 20%

---

## Step 1 — Identify the last stable version

### Via Git tags
```bash
# List all tags in reverse chronological order
git tag --sort=-version:refname

# Example output:
# v0.4.0
# v0.3.0
# v0.2.0
```

### Via GitLab Container Registry
- GitLab → your project → **Deploy** → **Container Registry**
- Look for the last stable image tag (e.g. `v0.3.0`)

### Via pipeline history
- GitLab → **CI/CD** → **Pipelines**
- Find the last green pipeline on `main`
- Note the commit SHA or tag

---

## Step 2 — Pull the stable image locally (optional verification)

```bash
# Replace v0.3.0 with the target version
docker pull registry.gitlab.com/zatadev/engineering-sandbox/user-service:v0.3.0

# Verify the image runs
docker run --rm -p 8080:8080 \
  registry.gitlab.com/zatadev/engineering-sandbox/user-service:v0.3.0
```

---

## Step 3 — Re-deploy the stable version

### Option A — Re-tag and push (triggers pipeline)

```bash
# Checkout the stable tag
git checkout v0.3.0

# Create a new tag pointing to this commit
git tag v0.3.1 -m "Rollback to v0.3.0 — hotfix"
git push origin v0.3.1
```

The pipeline will automatically build and push the image tagged `v0.3.1`.

### Option B — Re-tag the Docker image directly (faster, no pipeline)

```bash
# Pull the stable image
docker pull registry.gitlab.com/zatadev/engineering-sandbox/user-service:v0.3.0

# Re-tag as latest
docker tag \
  registry.gitlab.com/zatadev/engineering-sandbox/user-service:v0.3.0 \
  registry.gitlab.com/zatadev/engineering-sandbox/user-service:latest

# Push the re-tagged image
docker login registry.gitlab.com
docker push registry.gitlab.com/zatadev/engineering-sandbox/user-service:latest
```

---

## Step 4 — Verify the rollback

```bash
# Check the running version
curl http://localhost:8080/actuator/info

# Expected output includes the git tag:
# {
#   "git": {
#     "tags": "v0.3.0",
#     "branch": "main",
#     ...
#   }
# }

# Check health
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

---

## Step 5 — Post-rollback

- [ ] Notify the team that a rollback occurred
- [ ] Open a post-mortem issue in Linear
- [ ] Revert or fix the offending commit on `develop`
- [ ] Do **not** merge to `main` until root cause is identified

---

## Reference

| Command | Purpose |
|---|---|
| `git tag --sort=-version:refname` | List tags newest first |
| `git checkout v0.X.0` | Checkout a specific release |
| `docker pull registry.../user-service:vX.X.X` | Pull a specific image version |
| `docker tag source target` | Re-tag a Docker image |
| `curl /actuator/info` | Verify deployed version |
| `curl /actuator/health` | Verify service health |