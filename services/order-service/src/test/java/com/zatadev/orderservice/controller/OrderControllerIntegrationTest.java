package com.zatadev.orderservice.controller;

import com.zatadev.orderservice.AbstractIntegrationTest;
import com.zatadev.orderservice.config.JwtService;
import com.zatadev.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String TEST_CUSTOMER_ID = "00000000-0000-0000-0000-000000000002";
    private static final String TEST_PRODUCT_ID = "00000000-0000-0000-0000-000000000003";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    private HttpHeaders headersWithAuth() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtService.generateToken("test-user"));
        return headers;
    }

    @Test
    void createOrder_returns201() {
        String body = """
                {
                  "customerId": "%s",
                  "productId": "%s",
                  "quantity": 2,
                  "totalPrice": 49.99
                }
                """.formatted(TEST_CUSTOMER_ID, TEST_PRODUCT_ID);

        HttpEntity<String> entity = new HttpEntity<>(body, headersWithAuth());
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/orders", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void getOrders_returns200() {
        HttpEntity<Void> entity = new HttpEntity<>(headersWithAuth());
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuatorHealth_returns200() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
