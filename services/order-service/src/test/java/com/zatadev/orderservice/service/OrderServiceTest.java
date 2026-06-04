package com.zatadev.orderservice.service;

import com.zatadev.orderservice.domain.dto.CreateOrderRequest;
import com.zatadev.orderservice.domain.entity.Order;
import com.zatadev.orderservice.domain.entity.OrderStatus;
import com.zatadev.orderservice.exception.OrderCancellationException;
import com.zatadev.orderservice.exception.ResourceNotFoundException;
import com.zatadev.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID TEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEST_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TEST_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        pendingOrder = Order.builder()
                .id(TEST_ID)
                .customerId(TEST_CUSTOMER_ID)
                .productId(TEST_PRODUCT_ID)
                .quantity(2)
                .totalPrice(new BigDecimal("49.99"))
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findAll_returnsPageOfOrders() {
        var pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(pendingOrder)));

        var result = orderService.findAll(pageable);

        assertThat(result).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(TEST_ID);
    }

    @Test
    void findById_returnsOrder_whenExists() {
        when(orderRepository.findById(TEST_ID)).thenReturn(Optional.of(pendingOrder));

        var result = orderService.findById(TEST_ID);

        assertThat(result.id()).isEqualTo(TEST_ID);
        assertThat(result.customerId()).isEqualTo(TEST_CUSTOMER_ID);
    }

    @Test
    void findById_throwsException_whenNotFound() {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        when(orderRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    void create_savesAndReturnsOrder() {
        var request = new CreateOrderRequest(TEST_CUSTOMER_ID, TEST_PRODUCT_ID, 2, new BigDecimal("49.99"));
        when(orderRepository.save(any())).thenReturn(pendingOrder);

        var result = orderService.create(request);

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, times(1)).save(any());
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class), any(MessagePostProcessor.class));
    }

    @Test
    void cancel_changesStatus_whenPending() {
        when(orderRepository.findById(TEST_ID)).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any())).thenReturn(pendingOrder);

        var result = orderService.cancel(TEST_ID);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_throws_whenAlreadyCancelled() {
        pendingOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(TEST_ID)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.cancel(TEST_ID))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancel_throws_whenConfirmed() {
        pendingOrder.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(TEST_ID)).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.cancel(TEST_ID))
                .isInstanceOf(OrderCancellationException.class)
                .hasMessageContaining("confirmed");
    }
}
