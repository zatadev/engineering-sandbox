package com.zatadev.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zatadev.userservice.config.JwtService;
import com.zatadev.userservice.domain.dto.CreateUserRequest;
import com.zatadev.userservice.domain.dto.UpdateUserRequest;
import com.zatadev.userservice.domain.dto.UserResponse;
import com.zatadev.userservice.exception.ConflictException;
import com.zatadev.common.exception.ResourceNotFoundException;
import com.zatadev.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private UserResponse testUserResponse;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testUserResponse = UserResponse.builder()
                .id(testId)
                .username("zatadev")
                .email("zata@dev.com")
                .role("USER")
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class FindAll {

        @Test
        @WithMockUser
        @DisplayName("should return 200 with paginated users")
        void shouldReturn200() throws Exception {
            when(userService.findAll(any())).thenReturn(
                    new PageImpl<>(List.of(testUserResponse), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].username").value("zatadev"))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class FindById {

        @Test
        @WithMockUser
        @DisplayName("should return 200 when user found")
        void shouldReturn200WhenFound() throws Exception {
            when(userService.findById(testId)).thenReturn(testUserResponse);

            mockMvc.perform(get("/api/v1/users/{id}", testId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("zatadev"))
                    .andExpect(jsonPath("$.email").value("zata@dev.com"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(userService.findById(testId))
                    .thenThrow(new ResourceNotFoundException("User not found with id: " + testId));

            mockMvc.perform(get("/api/v1/users/{id}", testId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class Create {

        @Test
        @WithMockUser
        @DisplayName("should return 201 when user created")
        void shouldReturn201WhenCreated() throws Exception {
            CreateUserRequest request = new CreateUserRequest("zatadev", "zata@dev.com", "password123");
            when(userService.create(any())).thenReturn(testUserResponse);

            mockMvc.perform(post("/api/v1/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("zatadev"));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 when request is invalid")
        void shouldReturn400WhenInvalid() throws Exception {
            CreateUserRequest invalid = new CreateUserRequest("", "", "");

            mockMvc.perform(post("/api/v1/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 409 when username already exists")
        void shouldReturn409WhenConflict() throws Exception {
            CreateUserRequest request = new CreateUserRequest("zatadev", "zata@dev.com", "password123");
            when(userService.create(any()))
                    .thenThrow(new ConflictException("Username already exists: zatadev"));

            mockMvc.perform(post("/api/v1/users")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class Delete {

        @Test
        @WithMockUser
        @DisplayName("should return 204 when user deleted")
        void shouldReturn204WhenDeleted() throws Exception {
            doNothing().when(userService).delete(testId);

            mockMvc.perform(delete("/api/v1/users/{id}", testId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        @DisplayName("should return 404 when user not found")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("User not found with id: " + testId))
                    .when(userService).delete(testId);

            mockMvc.perform(delete("/api/v1/users/{id}", testId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}