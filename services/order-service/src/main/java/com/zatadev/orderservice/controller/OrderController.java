package com.zatadev.orderservice.controller;

import com.zatadev.orderservice.domain.dto.CancelOrderResponse;
import com.zatadev.orderservice.domain.dto.CreateOrderRequest;
import com.zatadev.orderservice.domain.dto.OrderResponse;
import com.zatadev.orderservice.service.OrderService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Timed(value = "order.controller.findAll", description = "GET /orders")
    public ResponseEntity<Page<OrderResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Timed(value = "order.controller.findById", description = "GET /orders/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @PostMapping
    @Timed(value = "order.controller.create", description = "POST /orders")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "order.controller.cancel", description = "DELETE /orders/{id}")
    public ResponseEntity<CancelOrderResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}