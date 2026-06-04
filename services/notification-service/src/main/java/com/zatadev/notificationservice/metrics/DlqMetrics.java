package com.zatadev.notificationservice.metrics;

import com.zatadev.notificationservice.config.RabbitMQConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class DlqMetrics {

    private final RabbitAdmin rabbitAdmin;
    private final MeterRegistry meterRegistry;

    public DlqMetrics(RabbitAdmin rabbitAdmin, MeterRegistry meterRegistry) {
        this.rabbitAdmin = rabbitAdmin;
        this.meterRegistry = meterRegistry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerDlqGauge() {
        Gauge.builder("rabbitmq.dlq.messages.ready",
                        this,
                        DlqMetrics::getDlqMessageCount)
                .description("Number of messages waiting in the notification DLQ")
                .tag("queue", RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED_DLQ)
                .register(meterRegistry);

        log.info("DLQ metrics gauge registered for queue: {}",
                RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED_DLQ);
    }

    private double getDlqMessageCount() {
        try {
            Properties props = rabbitAdmin.getQueueProperties(
                    RabbitMQConfig.QUEUE_NOTIFICATION_ORDER_CREATED_DLQ);
            if (props == null) {
                return 0;
            }
            Object count = props.get("QUEUE_MESSAGE_COUNT");
            return count instanceof Number n ? n.doubleValue() : 0;
        } catch (Exception e) {
            log.warn("Failed to get DLQ message count: {}", e.getMessage());
            return 0;
        }
    }
}