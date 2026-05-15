# ADR-006: Redis for Caching and Distributed State

## Status: Accepted

## Context

The sandbox services require a caching layer to demonstrate performance optimization
patterns and distributed state management. A decision is needed on the caching technology.

Alternatives considered:
- Redis — in-memory data structure store, widely used for caching and distributed state
- Caffeine — in-process JVM cache (no network hop, no distributed support)
- Memcached — simpler in-memory cache, fewer data structures than Redis
- No dedicated cache (database query optimization only)

## Decision

Use **Redis** as the caching and distributed state layer, integrated via Spring Cache
with `@Cacheable`, `@CacheEvict`, and `@CachePut` annotations.

Redis will run as a Docker container locally (Phase 2) and will be used to cache
frequently read data (e.g. user profiles) in user-service. Cache hit/miss ratios
will be observable via logs and metrics.

## Consequences

**Positive:**
- Redis is the industry standard for distributed caching in cloud-native architectures
- Spring Cache abstraction allows switching cache providers without changing business logic
- Supports TTL-based expiration, manual eviction, and pattern-based key management
- Beyond caching: Redis supports distributed locks, rate limiting, session storage —
  all relevant for senior-level interviews
- Observable behavior (hits/misses) makes caching decisions tangible and demonstrable
- Managed Redis (ElastiCache) maps directly to the AWS phase

**Negative:**
- Adds another infrastructure dependency to the local Docker Compose stack
- Cache invalidation strategy must be explicitly designed — no automatic consistency with DB
- Serialization of Java objects to Redis requires care (Jackson, Kryo, or custom serializers)

**Neutral:**
- Caffeine remains a valid choice for single-instance, in-process caching —
  Redis is chosen here specifically for its distributed and observable nature
- Cache invalidation is a cross-cutting concern that will be documented in a runbook
