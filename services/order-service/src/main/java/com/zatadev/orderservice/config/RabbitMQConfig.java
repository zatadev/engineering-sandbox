package com.zatadev.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String EXCHANGE = "sandbox.orders";

    // Routing keys
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";

    // Queues
    public static final String QUEUE_NOTIFICATION_ORDER_CREATED = "notification.order.created";
    public static final String QUEUE_NOTIFICATION_ORDER_CREATED_DLQ = "notification.order.created.dlq";

    // Dead letter exchange
    public static final String EXCHANGE_DLQ = "sandbox.orders.dlq";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLQ, true, false);
    }

    @Bean
    public Queue notificationOrderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_ORDER_CREATED)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", QUEUE_NOTIFICATION_ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Queue notificationOrderCreatedDlq() {
        return QueueBuilder.durable(QUEUE_NOTIFICATION_ORDER_CREATED_DLQ)
                .build();
    }

    @Bean
    public Binding notificationOrderCreatedBinding(
            Queue notificationOrderCreatedQueue,
            TopicExchange ordersExchange) {
        return BindingBuilder
                .bind(notificationOrderCreatedQueue)
                .to(ordersExchange)
                .with(ROUTING_KEY_ORDER_CREATED);
    }

    @Bean
    public Binding deadLetterBinding(
            Queue notificationOrderCreatedDlq,
            DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(notificationOrderCreatedDlq)
                .to(deadLetterExchange)
                .with(QUEUE_NOTIFICATION_ORDER_CREATED_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}