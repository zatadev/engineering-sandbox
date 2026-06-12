package com.zatadev.notificationservice.messaging;

import com.zatadev.order.contracts.OrderCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderEventListener {

    private final IdempotencyService idempotencyService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "order.created",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreated(
            ConsumerRecord<String, OrderCreatedEvent> record,
            Acknowledgment ack) {

        OrderCreatedEvent event = record.value();
        String correlationId = extractCorrelationId(record);
        MDC.put("correlationId", correlationId);

        try {
            log.info("Received Kafka OrderCreatedEvent: eventId={}, orderId={}, partition={}, offset={}",
                    event.eventId(), event.orderId(), record.partition(), record.offset());

            if (idempotencyService.isDuplicate(event.eventId())) {
                meterRegistry.counter("notifications.idempotency.skipped", "source", "kafka").increment();
                ack.acknowledge();
                return;
            }

            sendNotification(event);
            meterRegistry.counter("notifications.sent", "source", "kafka").increment();
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Failed to process Kafka OrderCreatedEvent: eventId={}, error={}",
                    event.eventId(), ex.getMessage());
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void sendNotification(OrderCreatedEvent event) {
        log.info("Kafka notification sent to customerId={} for orderId={} — quantity={}, total={}",
                event.customerId(),
                event.orderId(),
                event.quantity(),
                event.totalPrice());
    }

    private String extractCorrelationId(ConsumerRecord<String, OrderCreatedEvent> record) {
        var header = record.headers().lastHeader("X-Correlation-ID");
        if (header != null) {
            return new String(header.value());
        }
        return java.util.UUID.randomUUID().toString();
    }
}