package com.zatadev.orderservice.repository;

import com.zatadev.orderservice.domain.entity.Order;
import com.zatadev.orderservice.domain.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);
}
