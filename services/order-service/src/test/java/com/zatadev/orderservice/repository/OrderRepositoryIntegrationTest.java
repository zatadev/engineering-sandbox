package com.zatadev.orderservice.repository;

import com.zatadev.orderservice.AbstractIntegrationTest;
import com.zatadev.orderservice.domain.entity.Order;
import com.zatadev.orderservice.domain.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final UUID CUSTOMER_A = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final UUID CUSTOMER_B = UUID.fromString("00000000-0000-0000-0000-000000000043");
    private static final UUID PRODUCT_A = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void save_andFindById_works() {
        Order order = Order.builder()
                .customerId(CUSTOMER_A)
                .productId(PRODUCT_A)
                .quantity(3)
                .totalPrice(new BigDecimal("99.99"))
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(orderRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void findByCustomerId_returnsPaginatedResults() {
        Order order = Order.builder()
                .customerId(CUSTOMER_A)
                .productId(PRODUCT_A)
                .quantity(1)
                .totalPrice(new BigDecimal("10.00"))
                .status(OrderStatus.PENDING)
                .build();
        orderRepository.save(order);

        var page = orderRepository.findByCustomerId(CUSTOMER_A, PageRequest.of(0, 10));

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getCustomerId()).isEqualTo(CUSTOMER_A);
    }

    @Test
    void findByStatus_returnsMatchingOrders() {
        Order order = Order.builder()
                .customerId(CUSTOMER_B)
                .productId(PRODUCT_A)
                .quantity(1)
                .totalPrice(new BigDecimal("5.00"))
                .status(OrderStatus.CONFIRMED)
                .build();
        orderRepository.save(order);

        var results = orderRepository.findByStatus(OrderStatus.CONFIRMED);

        assertThat(results).anyMatch(o -> o.getCustomerId().equals(CUSTOMER_B));
    }
}
