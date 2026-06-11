# ADR-011: RabbitMQ vs Kafka — When to Use Which

## Status: Accepted

## Context

Phase 4 introduced two messaging systems in parallel:
- **RabbitMQ** — used for the `order.created` flow between order-service and notification-service (ZAT-88)
- **Kafka** — added as a parallel flow on the same event (ZAT-92)

Both were implemented intentionally to demonstrate their differences in practice
and to document when each should be chosen in a production environment.

This ADR captures the decision rationale and serves as a reference for future
architectural choices in this sandbox and in real-world projects.

---

## RabbitMQ — Use Cases and Characteristics

### What RabbitMQ is

RabbitMQ is a **message broker**. Its core model is push-based: the broker
actively delivers messages to consumers. Messages are removed from the queue
once acknowledged.

### When to use RabbitMQ

**Complex routing logic**
RabbitMQ's exchange model (Direct, Topic, Fanout, Headers) allows sophisticated
routing without consumer-side filtering. A single producer can route messages to
different queues based on routing keys, binding patterns, or message headers.

```
order.created   → notification queue
order.cancelled → refund queue + audit queue (fanout)
order.*.fr      → french-localized-notification queue (topic pattern)
```

**Task queues and work distribution**
RabbitMQ excels at distributing tasks across worker instances. Multiple consumers
on the same queue share the load — each message is processed by exactly one consumer.
Ideal for: email sending, PDF generation, payment processing.

**Native retry and dead-letter logic**
RabbitMQ supports dead-letter exchanges (DLX) natively at the queue level via
`x-dead-letter-exchange`. Combined with per-message TTL and reject/nack semantics,
retry logic is built into the broker — no consumer-side retry framework needed.

**Low to medium volume**
RabbitMQ is optimized for throughput in the thousands to tens of thousands of
messages per second range. Beyond that, operational complexity increases.

**Request/Reply patterns**
RabbitMQ supports synchronous-style RPC over messaging via `replyTo` queues and
`correlationId` headers — useful when a response is expected from a consumer.

### RabbitMQ characteristics summary

| Property | Behavior |
|----------|----------|
| Message retention | Deleted after acknowledgment |
| Consumer model | Push — broker delivers to consumer |
| Ordering | Per-queue FIFO (not globally guaranteed) |
| Replay | Not supported natively |
| Routing | Rich — Direct, Topic, Fanout, Headers exchanges |
| Retry | Native via DLX + TTL |
| Protocol | AMQP 0-9-1 |
| Operational complexity | Low to medium |

### Implementation in this sandbox

- Exchange: `sandbox.orders` (TopicExchange)
- Queue: `notification.order.created` with DLX configured
- Dead-letter exchange: `sandbox.orders.dlq` (DirectExchange)
- Dead-letter queue: `notification.order.created.dlq`
- Retry: 3 attempts, exponential backoff (1s → 2s → 4s), max 10s
- Correlation ID propagated via AMQP message headers

---

## Kafka — Use Cases and Characteristics

### What Kafka is

Kafka is a **distributed event streaming platform**. Its core model is pull-based:
consumers poll the broker at their own pace. Messages are retained on disk for a
configurable period, regardless of whether they have been consumed.

### When to use Kafka

**Event streaming and high throughput**
Kafka is designed for millions of events per second. It achieves this through
partitioned, append-only logs and sequential disk I/O. Ideal for: clickstream
data, IoT telemetry, financial transactions at scale.

**Event replay and reprocessing**
Because Kafka retains messages, any consumer can replay the full history of a
topic. This enables:
- Rebuilding read models after a bug fix
- Onboarding a new service that needs historical data
- A/B testing a new consumer against past events

**Audit log and event sourcing**
The immutable, ordered log makes Kafka a natural fit for audit trails. Every
state change is recorded as an event — the current state can always be
reconstructed by replaying from the beginning.

**Multiple independent consumer groups**
Multiple services can consume the same topic independently, each maintaining
their own offset. Adding a new consumer does not affect existing ones and
requires no broker configuration change.

**Consumer-controlled pace**
Pull-based consumption means consumers are never overwhelmed by the broker.
A slow consumer simply lags behind — it does not cause back-pressure or
message loss.

### Kafka characteristics summary

| Property | Behavior |
|----------|----------|
| Message retention | Time-based or size-based (configurable, default 7 days) |
| Consumer model | Pull — consumer polls at its own pace |
| Ordering | Guaranteed within a partition |
| Replay | Native — consumers can seek to any offset |
| Routing | By topic and partition key |
| Retry | Consumer-side (no native DLX — use Dead Letter Topic pattern) |
| Protocol | Kafka binary protocol |
| Operational complexity | Medium to high |

### Partitioning and ordering

Messages within a partition are strictly ordered. The partition key determines
which partition receives the message.

```
topic: order.created — 3 partitions

Partition 0: [order-A-v1, order-A-v2, order-A-v3]  ← all events for order-A
Partition 1: [order-B-v1, order-B-v2]
Partition 2: [order-C-v1]
```

In this sandbox, `orderId` is used as the partition key — all events for the same
order land in the same partition, guaranteeing per-order ordering.

### Consumer groups and load balancing

Each partition is assigned to exactly one consumer instance within a group.
Scaling a consumer group is automatic — adding instances triggers a rebalance.

```
topic: order.created — 3 partitions
consumer group: notification-service — 3 instances

Instance 1 → Partition 0
Instance 2 → Partition 1
Instance 3 → Partition 2
```

### Offset management

In this sandbox, manual offset commit is used (`enable-auto-commit: false`).
The consumer commits the offset explicitly after successful processing via
`ack.acknowledge()`. This guarantees at-least-once delivery — if the consumer
crashes mid-processing, the message is redelivered.

### Implementation in this sandbox

- Topic: `order.created` — 3 partitions, replication factor 1 (single node)
- Partition key: `orderId`
- Consumer group: `notification-service`
- Offset reset: `earliest` (reads from beginning on first start)
- Ack mode: `MANUAL` — explicit `ack.acknowledge()` after processing
- Correlation ID propagated via Kafka record headers
- KRaft mode (no Zookeeper)

---

## Decision — When to Choose Which in Production

### Choose RabbitMQ when

- You need **complex routing** (topic patterns, header-based routing)
- You need **native dead-letter and retry** without custom code
- The use case is **task-oriented** (do this work once, then discard)
- Volume is **moderate** (< 100k messages/second)
- You need **request/reply** semantics
- The team is more familiar with traditional message brokers

**Production examples:** email/SMS notifications, payment job queues, PDF
generation tasks, webhook delivery with retry.

### Choose Kafka when

- You need **event replay** or **audit log**
- You need **multiple independent consumers** on the same event stream
- Volume is **high** (> 100k messages/second, or growing)
- You are building **event sourcing** or **CQRS** architectures
- You need **long-term retention** of events
- You need **exactly-once** semantics (Kafka Transactions)

**Production examples:** user activity tracking, financial audit trails,
microservices synchronization via CDC (Change Data Capture), real-time analytics.

### Decision matrix

| Criteria | RabbitMQ | Kafka |
|----------|----------|-------|
| Complex routing | ✅ Native | ❌ Topic-only |
| Native DLQ/retry | ✅ Native | ⚠️ Dead Letter Topic pattern |
| Message replay | ❌ Not supported | ✅ Native |
| Multiple consumers | ⚠️ Competing consumers | ✅ Consumer groups |
| High throughput | ⚠️ Limited | ✅ Designed for it |
| Audit log | ❌ Messages deleted | ✅ Immutable log |
| Operational simplicity | ✅ Simpler | ⚠️ More complex |
| Request/Reply | ✅ Native | ❌ Not idiomatic |

### In a real production system

Many mature architectures use **both**:

- **Kafka** as the system of record — event stream, audit log, service sync
- **RabbitMQ** for operational tasks — notifications, retries, job queues

The two are not mutually exclusive. The question is not "which one" but
"which one for this specific flow".

---

## Messaging Patterns Applied in This Sandbox

### At-least-once delivery and idempotence

Both RabbitMQ and Kafka guarantee **at-least-once delivery** — a message can be
redelivered on consumer crash, network timeout, or manual requeue.

Without idempotence, this causes:
- Duplicate notifications sent to customers
- Duplicate payments processed
- In financial systems: regulatory non-compliance

**Pattern applied:** each `OrderCreatedEvent` carries a unique `eventId` generated
by the producer. Before processing, the consumer checks Redis via `SETNX` with a
24-hour TTL. If the key already exists, the event is a duplicate and is silently
dropped. The same `IdempotencyService` protects against duplicates from both
RabbitMQ and Kafka consumers.

### Correlation ID propagation

A `correlationId` is generated at the HTTP layer (via `CorrelationIdFilter`) and
injected into the MDC. It is propagated through:
- RabbitMQ: as an AMQP message header (`X-Correlation-ID`)
- Kafka: as a Kafka record header (`X-Correlation-ID`)

Consumers extract the header and inject it back into MDC before processing.
This enables end-to-end tracing across synchronous HTTP and asynchronous
messaging without a distributed tracing framework.

### Dead-letter queues (RabbitMQ)

After 3 failed processing attempts with exponential backoff, messages are
automatically routed to `notification.order.created.dlq` via the
`sandbox.orders.dlq` dead-letter exchange. The DLQ message count is exposed
as a Micrometer gauge (`rabbitmq.dlq.messages.ready`) for alerting.

The equivalent Kafka pattern — Dead Letter Topic — is not implemented in this
sandbox but would follow the same principle using a `_dlq` suffix topic.

---

## Amendment to ADR-008

ADR-008 (Phase 1) described error handling using a custom `ErrorResponse` DTO.
This was superseded during Phase 4:

- `ErrorResponse` replaced by `ProblemDetail` (RFC 7807, Spring Boot 3 native)
- `ErrorType` enum centralizes HTTP status and title mappings
- `about:blank` used as default `type` (no fake documentation URLs)
- `BaseGlobalExceptionHandler` extracted to `services/common` (ZAT-174)
- `MethodArgumentNotValidException` handler moved to base class

Exception catalogue by service:

| Service | Exceptions |
|---------|-----------|
| user-service | NOT_FOUND, CONFLICT, VALIDATION, INTERNAL |
| order-service | NOT_FOUND, ORDER_CANCELLATION, VALIDATION, INTERNAL |
| notification-service | VALIDATION, INTERNAL |

`NOT_FOUND`, `VALIDATION`, and `INTERNAL` are defined in
`com.zatadev.common.exception.ErrorType`. Service-specific exceptions
(`ORDER_CANCELLATION`, `CONFLICT`) remain in each service's local `ErrorType`.

---

## References

- [RabbitMQ Documentation](https://www.rabbitmq.com/docs)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kafka vs RabbitMQ — Confluent](https://www.confluent.io/blog/kafka-vs-rabbitmq/)
- ADR-005: RabbitMQ before Kafka — progressive complexity
- ADR-008: DTO separation and error handling strategy
- `services/order-service/src/main/java/com/zatadev/orderservice/config/RabbitMQConfig.java`
- `services/order-service/src/main/java/com/zatadev/orderservice/config/KafkaConfig.java`
- `services/notification-service/src/main/java/com/zatadev/notificationservice/messaging/`
