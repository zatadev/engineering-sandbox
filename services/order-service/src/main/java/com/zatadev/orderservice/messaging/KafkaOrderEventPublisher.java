package com.zatadev.orderservice.messaging;

import com.zatadev.orderservice.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        // Use orderId as partition key — guarantees ordering per order
        String partitionKey = event.orderId().toString();
        String correlationId = MDC.get("correlationId");

        ProducerRecord<String, OrderCreatedEvent> record = new ProducerRecord<>(
                KafkaConfig.TOPIC_ORDER_CREATED,
                partitionKey,
                event
        );

        if (correlationId != null) {
            record.headers().add("X-Correlation-ID", correlationId.getBytes());
        }

        CompletableFuture<SendResult<String, OrderCreatedEvent>> future =
                kafkaTemplate.send(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderCreatedEvent to Kafka: orderId={}, error={}",
                        event.orderId(), ex.getMessage());
            } else {
                log.info("OrderCreatedEvent published to Kafka: orderId={}, partition={}, offset={}",
                        event.orderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}