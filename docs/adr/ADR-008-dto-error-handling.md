# ADR-008: DTO Separation and Error Handling Strategy

## Status: Accepted

## Context

During Phase 1 development of `user-service`, two cross-cutting design decisions
were made that affect all REST endpoints and will apply to all future services:

1. How to decouple the internal domain model from the API contract
2. How to handle and format errors consistently across all endpoints

These decisions needed to be documented to ensure consistency across Phase 4
services (`order-service`, `notification-service`) and to serve as a reference
for onboarding.

---

## Decision 1: Entity / DTO Separation

### Decision
JPA entities are never exposed directly via REST endpoints.
All API inputs and outputs use dedicated DTO classes (records).

### Structure
```
domain/
├── entity/     → JPA entities (User) — internal, never serialized to API
└── dto/        → DTOs (UserResponse, CreateUserRequest) — API contract
```

### Rationale

**Security** — Entities often contain sensitive fields (password hash, internal
flags). Exposing them directly risks leaking data unintentionally. DTOs make
the exposure explicit and deliberate.

**Stability** — The API contract (DTOs) can evolve independently from the
database schema (entities). Adding an internal field to an entity does not
automatically expose it to API consumers.

**Validation** — Input validation (`@NotBlank`, `@Email`, `@Size`) belongs on
request DTOs, not on entities. Entities represent persisted state, not API
input constraints.

**Serialization control** — DTOs use Java records, which are immutable by
design. This prevents accidental mutation of API responses during construction.

### Implementation
- Request DTOs: `CreateUserRequest`, `UpdateUserRequest`, `LoginRequest`
- Response DTOs: `UserResponse`, `AuthResponse`
- Mapping: done in `UserService.toResponse()` — kept close to the business logic
- No MapStruct in Phase 1 (added complexity not justified at this scale)

### Consequences
- Every new field exposed via API requires an explicit DTO change
- Mapping code must be maintained when entity structure changes
- Future phases may introduce MapStruct if mapping becomes complex

---

## Decision 2: Global Error Handling with @ControllerAdvice

### Decision
All exceptions are handled centrally via a single `GlobalExceptionHandler`
annotated with `@RestControllerAdvice`. Controllers never catch exceptions
themselves.

### Rationale

**Consistency** — All API errors follow the same format regardless of which
controller or service throws them. Consumers can rely on a predictable error
structure.

**Separation of concerns** — Controllers focus on routing and delegation.
Error formatting is a cross-cutting concern handled separately.

**Completeness** — A catch-all `Exception` handler ensures no unhandled
exception ever reaches the client as a raw stacktrace.

### Exception hierarchy
```
ResourceNotFoundException       → 404 Not Found
ConflictException               → 409 Conflict
MethodArgumentNotValidException → 400 Bad Request (Bean Validation)
Exception (catch-all)           → 500 Internal Server Error
```

### Log strategy
- Business errors (404, 409, 400): `log.warn` — expected, not actionable
- Unexpected errors (500): `log.error` with full stacktrace — requires investigation

---

## Decision 3: Error Response Format

### Decision
Error responses follow a simplified version of
[RFC 7807 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807).

### Format
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "path": "/api/v1/users/123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2026-05-20T15:30:00"
}
```

### Rationale

**RFC 7807 alignment** — Using a standard format reduces the learning curve
for API consumers and aligns with industry practice. Full RFC 7807 compliance
(`application/problem+json` content type, `type` URI) was considered but
deferred to Phase 8 as it adds complexity without immediate value.

**Debuggability** — Including `path` and `timestamp` allows correlating errors
with logs without requiring the consumer to have access to server logs.

**No stacktraces** — The 500 handler returns a generic message. Stacktraces
are logged server-side but never sent to the client (security and clarity).

### Consequences
- All services in this sandbox must use the same `ErrorResponse` format
- Phase 8 security review may upgrade to full RFC 7807 compliance
  (`application/problem+json` media type)

---

## Alternatives Considered

| Option | Rejected because |
|--------|-----------------|
| Expose JPA entities directly | Security risk, tight coupling between DB schema and API contract |
| Per-controller exception handling | Inconsistent error formats, code duplication |
| Spring's default error format (`/error`) | Verbose, includes stacktrace in dev, not standardized |
| Full RFC 7807 | Adds `type` URI management complexity, deferred to Phase 8 |
| MapStruct for mapping | Over-engineering at this scale, manual mapping is explicit and readable |

---

---

## Decision 4: Shared `common` Module for Cross-cutting Infrastructure

**Date:** 2026-06-17
**Status:** Accepted

### Decision

Cross-cutting infrastructure components are extracted to `services/common` and shared
across all services via Spring Boot auto-configuration. Each service declares `common`
as a Maven dependency; no service re-declares these components locally.

### What is shared via `common`

| Component | Mechanism |
|---|---|
| `CorrelationIdFilter` | Registered as `@Bean` by `CommonAutoConfiguration` |
| `MetricsConfig` | Imported via `@Import` in `CommonAutoConfiguration` |
| `ResourceNotFoundException` | Plain library class, used directly |
| `BaseGlobalExceptionHandler` | Extended by each service's `GlobalExceptionHandler` |
| `logback-spring.xml` | Classpath resource, loaded by Spring Boot's Logback integration |

### What stays per service (intentional)

| Component | Reason |
|---|---|
| `ErrorType` enum | Domain-specific entries (`CONFLICT`, `ORDER_CANCELLATION`) differ per service |
| `GlobalExceptionHandler` concrete class | Extends the base and adds service-specific exception mappings |

### Inheritance pattern

```java
// common
@RestControllerAdvice
public class BaseGlobalExceptionHandler {
    // handles ResourceNotFoundException, MethodArgumentNotValidException, Exception
}

// per service
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseGlobalExceptionHandler {
    // handles only service-specific exceptions (ConflictException, OrderCancellationException)
}
```

### Rationale

Sharing `ErrorType` would require a common superset of all service error codes, coupling
unrelated services. Keeping `ErrorType` local preserves domain boundaries. The base
handler covers the universal cases (404, 400, 500); each service extends it for its own
domain exceptions only.

---

## References

- [RFC 7807 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc7807)
- Spring `@RestControllerAdvice` documentation
- `BaseGlobalExceptionHandler.java` — `services/common/src/main/java/com/zatadev/common/exception/`
- `GlobalExceptionHandler.java` — `<service>/src/main/java/com/zatadev/<service>/exception/`
