package com.zatadev.orderservice.controller;

import com.zatadev.orderservice.config.JwtService;
import com.zatadev.orderservice.domain.dto.OrderResponse;
import com.zatadev.orderservice.domain.entity.OrderStatus;
import com.zatadev.orderservice.exception.GlobalExceptionHandler;
import com.zatadev.orderservice.exception.ResourceNotFoundException;
import com.zatadev.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    private static final UUID TEST_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEST_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TEST_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID UNKNOWN_ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    private OrderResponse sampleResponse() {
        return OrderResponse.builder()
                .id(TEST_ORDER_ID)
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
    @WithMockUser(roles = "USER")
    void findAll_returns200() throws Exception {
        when(orderService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_ORDER_ID.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void findById_returns200_whenFound() throws Exception {
        when(orderService.findById(TEST_ORDER_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/orders/" + TEST_ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(TEST_CUSTOMER_ID.toString()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void findById_returns404_whenNotFound() throws Exception {
        when(orderService.findById(UNKNOWN_ORDER_ID))
                .thenThrow(new ResourceNotFoundException("Order not found with id: " + UNKNOWN_ORDER_ID));

        mockMvc.perform(get("/api/v1/orders/" + UNKNOWN_ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_returns201() throws Exception {
        when(orderService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "productId": "%s",
                                  "quantity": 2,
                                  "totalPrice": 49.99
                                }
                                """.formatted(TEST_CUSTOMER_ID, TEST_PRODUCT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void create_returns400_whenInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }
}
