# ADR-003: Java 21 + Spring Boot 3 as Core Backend Stack

## Status: Accepted

## Context

The sandbox requires a backend language and framework for building production-grade REST services.
The choice must reflect the current state of the Swiss and European enterprise job market,
be well-supported for cloud-native patterns, and align with the author's existing expertise
(~8 years of Java in banking/finance environments).

Alternatives considered:
- Java 17 (previous LTS)
- Java 21 (current LTS)
- Kotlin + Spring Boot
- Quarkus or Micronaut as alternative frameworks

## Decision

Use **Java 21** with **Spring Boot 3** as the core backend stack.

Build tooling will be Maven (primary) or Gradle depending on the service, with preference
for Maven for familiarity in enterprise contexts.

## Consequences

**Positive:**
- Java 21 is the current LTS — the right choice for any new production system started in 2024+
- Spring Boot 3 requires Java 17+ and brings native support for virtual threads (Project Loom),
  improved observability via Micrometer, and better GraalVM native image support
- Virtual threads (Java 21) enable high-concurrency without reactive programming complexity
- Spring ecosystem (Security, Data JPA, Actuator, Cache) covers all phases of this sandbox
- Dominant stack in Swiss banking and enterprise — maximizes interview relevance
- Existing expertise reduces ramp-up time and allows focus on cloud-native concerns

**Negative:**
- Spring Boot has higher baseline memory footprint compared to Quarkus or Micronaut
- Reactive programming (WebFlux) is not the default — imperative style limits some patterns
- Heavier framework compared to lightweight alternatives

**Neutral:**
- Kotlin remains a viable future enhancement — Spring Boot 3 supports it natively
- GraalVM native image compilation is available but not in scope for this sandbox
