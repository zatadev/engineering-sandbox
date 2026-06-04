package com.zatadev.notificationservice.messaging;

import com.zatadev.notificationservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;
import org.slf4j.MDC;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event, Message message) {
        String correlationId = extractCorrelationId(message);
        MDC.put("correlationId", correlationId);

        try {
            log.info("Received OrderCreatedEvent: eventId={}, orderId={}, customerId={}",
                    event.eventId(),
                    event.orderId(),
                    event.customerId());

            if (idempotencyService.isDuplicate(event.eventId())) {
                return;
            }

            sendNotification(event);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String extractCorrelationId(Message message) {
        Object header = message.getMessageProperties()
                .getHeader("X-Correlation-ID");
        if (header instanceof String s && !s.isBlank()) {
            return s;
        }
        // Fallback: generate a new one if not present
        return java.util.UUID.randomUUID().toString();
    }

    private void sendNotification(OrderCreatedEvent event) {
        // Stub — real dispatch (SendGrid, Twilio, etc.) is out of scope for this sandbox
        log.info("Notification sent to customerId={} for orderId={} — quantity={}, total={}",
                event.customerId(),
                event.orderId(),
                event.quantity(),
                event.totalPrice());
    }
}
