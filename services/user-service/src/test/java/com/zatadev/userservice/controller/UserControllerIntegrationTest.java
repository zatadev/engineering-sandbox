package com.zatadev.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zatadev.userservice.AbstractIntegrationTest;
import com.zatadev.userservice.domain.dto.CreateUserRequest;
import com.zatadev.userservice.domain.dto.UpdateUserRequest;
import com.zatadev.userservice.domain.entity.User;
import com.zatadev.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserController Integration")
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createAdminUser();
        authToken = obtainToken();
    }

    private void createAdminUser() {
        User admin = User.builder()
                .username("admin")
                .email("admin@test.com")
                .password("admin")
                .role("ADMIN")
                .active(true)
                .build();
        userRepository.save(admin);
    }

    private String obtainToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"username\":\"admin\",\"password\":\"admin\"}";

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(body, headers),
                String.class
        );

        System.out.println("Login status: " + response.getStatusCode());
        System.out.println("Login body: " + response.getBody());

        if (response.getStatusCode() == HttpStatus.OK) {
            // parse manually
            try {
                return objectMapper.readTree(response.getBody()).get("accessToken").asText();
            } catch (Exception e) {
                System.out.println("Parse error: " + e.getMessage());
            }
        }
        return null;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authToken != null) {
            headers.setBearerAuth(authToken);
        }
        return headers;
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateUser {

        @Test
        @DisplayName("should create user and return 201")
        void shouldCreateUser() {
            CreateUserRequest request = new CreateUserRequest("zatadev", "zata@dev.com", "password123");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/users",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders()),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).contains("zatadev");
        }

        @Test
        @DisplayName("should return 409 when username already exists")
        void shouldReturn409WhenConflict() {
            CreateUserRequest request = new CreateUserRequest("zatadev", "zata@dev.com", "password123");

            restTemplate.exchange("/api/v1/users", HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders()), String.class);

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/users",
                    HttpMethod.POST,
                    new HttpEntity<>(request, authHeaders()),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("should return 400 when request is invalid")
        void shouldReturn400WhenInvalid() {
            CreateUserRequest invalid = new CreateUserRequest("", "", "");

            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/users",
                    HttpMethod.POST,
                    new HttpEntity<>(invalid, authHeaders()),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetUsers {

        @Test
        @DisplayName("should return 200 with empty page")
        void shouldReturnEmptyPage() {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/users",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders()),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("totalElements");
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/api/v1/users", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteUser {

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenNotFound() {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/users/" + java.util.UUID.randomUUID(),
                    HttpMethod.DELETE,
                    new HttpEntity<>(authHeaders()),
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}