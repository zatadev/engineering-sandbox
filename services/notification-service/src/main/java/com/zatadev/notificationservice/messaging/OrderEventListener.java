package com.zatadev.notificationservice.messaging;

import com.zatadev.notificationservice.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: eventId={}, orderId={}, customerId={}",
                event.eventId(),
                event.orderId(),
                event.customerId());

        // Simulate notification dispatch
        sendNotification(event);
    }

    private void sendNotification(OrderCreatedEvent event) {
        // STUB — simulate email/SMS notification
        // Real implementation (SendGrid, Twilio, etc.) out of scope for this sandbox
        log.info("Notification sent to customerId={} for orderId={} — quantity={}, total={}",
                event.customerId(),
                event.orderId(),
                event.quantity(),
                event.totalPrice());
    }
}
