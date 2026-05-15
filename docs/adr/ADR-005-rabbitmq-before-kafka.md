# ADR-005: RabbitMQ Before Kafka

## Status: Accepted

## Context

Phase 4 introduces event-driven architecture across three services (user-service,
order-service, notification-service). A decision is needed on which messaging broker
to introduce first, and whether to use both.

Both RabbitMQ and Kafka are in the target stack. They serve different architectural
purposes and are not direct substitutes for each other.

Alternatives considered:
- RabbitMQ only
- Kafka only
- Both, introduced simultaneously
- Both, introduced sequentially (RabbitMQ first, then Kafka)

## Decision

Introduce **RabbitMQ first**, then add **Kafka** in the same phase as a second layer.

RabbitMQ will be used for task-queue and direct messaging patterns (e.g. order events
triggering notifications). Kafka will be used to demonstrate log-based streaming,
consumer groups, and offset management on the same or adjacent use case.

## Consequences

**Positive:**
- RabbitMQ has a lower conceptual entry point — exchanges, queues, bindings are easier
  to reason about before introducing Kafka's partition and offset model
- Progressive complexity: mastering RabbitMQ first makes Kafka's design decisions clearer
- Both brokers are represented — demonstrates awareness of when to use which
- Dead-letter queues and retry logic are more straightforward to implement in RabbitMQ first
- Kafka expertise is increasingly required in senior/lead roles — including it is essential

**Negative:**
- Running both brokers locally increases resource usage in Docker Compose
- Maintaining two integration patterns adds surface area to the codebase
- Risk of conflating the two mental models if introduced too close together

**Neutral:**
- RabbitMQ: best for task queues, routing, fan-out, request/reply patterns
- Kafka: best for event streaming, audit logs, high-throughput pipelines, replay
- The ADR itself becomes a strong interview talking point on broker selection criteria
