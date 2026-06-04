package com.zatadev.notificationservice.messaging;

import com.zatadev.notificationservice.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final IdempotencyService idempotencyService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: eventId={}, orderId={}, customerId={}",
                event.eventId(),
                event.orderId(),
                event.customerId());

        if (idempotencyService.isDuplicate(event.eventId())) {
            return;
        }
        sendNotification(event);
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
