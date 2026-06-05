package com.zatadev.orderservice.service;

import com.zatadev.orderservice.config.RabbitMQConfig;
import com.zatadev.orderservice.domain.dto.CancelOrderResponse;
import com.zatadev.orderservice.domain.dto.CreateOrderRequest;
import com.zatadev.orderservice.domain.dto.OrderResponse;
import com.zatadev.orderservice.domain.entity.Order;
import com.zatadev.orderservice.domain.entity.OrderStatus;
import com.zatadev.orderservice.exception.OrderCancellationException;
import com.zatadev.orderservice.exception.ResourceNotFoundException;
import com.zatadev.orderservice.messaging.KafkaOrderEventPublisher;
import com.zatadev.orderservice.messaging.OrderCreatedEvent;
import com.zatadev.orderservice.repository.OrderRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessagePostProcessor;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final KafkaOrderEventPublisher kafkaOrderEventPublisher;

    @Timed(value = "order.service.findAll", description = "Paginated order listing")
    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        log.debug("Fetching all orders, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @Timed(value = "order.service.findById", description = "Order lookup by ID")
    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        log.debug("Fetching order id={}", id);
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    @Timed(value = "order.service.create", description = "Order creation")
    @Counted(value = "order.service.created.total", description = "Orders created")
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        log.info("Creating order for customerId={}, productId={}", request.customerId(), request.productId());
        Order order = Order.builder()
                .customerId(request.customerId())
                .productId(request.productId())
                .quantity(request.quantity())
                .totalPrice(request.totalPrice())
                .status(OrderStatus.PENDING)
                .build();
        Order saved = orderRepository.save(order);
        log.info("Order created id={}", saved.getId());

        // Published inside @Transactional — Outbox Pattern needed before production.
        OrderCreatedEvent event = OrderCreatedEvent.from(saved);

        // Publish via RabbitMQ
        // Propagate correlationId through message headers
        String correlationId = MDC.get("correlationId");
        MessagePostProcessor messagePostProcessor = message -> {
            if (correlationId != null) {
                message.getMessageProperties()
                        .setHeader("X-Correlation-ID", correlationId);
            }
            return message;
        };
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_ORDER_CREATED,
                event,
                messagePostProcessor
        );
        log.info("OrderCreatedEvent published to RabbitMQ: orderId={}", saved.getId());

        // Publish via Kafka
        kafkaOrderEventPublisher.publishOrderCreated(event);

        return OrderResponse.from(saved);
    }

    @Timed(value = "order.service.cancel", description = "Order cancellation")
    @Transactional
    public CancelOrderResponse cancel(UUID id) {
        log.info("Cancelling order id={}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderCancellationException("Order " + id + " is already cancelled");
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new OrderCancellationException("Order " + id + " cannot be cancelled once confirmed");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order cancelled id={}", id);

        return CancelOrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .updatedAt(order.getUpdatedAt())
                .message("Order successfully cancelled")
                .build();
    }
}
