package com.zatadev.userservice.controller;

import com.zatadev.userservice.domain.dto.CreateUserRequest;
import com.zatadev.userservice.domain.dto.UpdateUserRequest;
import com.zatadev.userservice.domain.dto.UserResponse;
import com.zatadev.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.micrometer.core.annotation.Timed;
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Timed(value = "users.requests", description = "Time taken to process user requests", percentiles = {0.5, 0.95, 0.99})
    @GetMapping
    public ResponseEntity<Page<UserResponse>> findAll(
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @Timed(value = "users.requests", description = "Time taken to process user requests", percentiles = {0.5, 0.95, 0.99})
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Timed(value = "users.requests", description = "Time taken to process user requests", percentiles = {0.5, 0.95, 0.99})
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(request));
    }

    @Timed(value = "users.requests", description = "Time taken to process user requests", percentiles = {0.5, 0.95, 0.99})
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Timed(value = "users.requests", description = "Time taken to process user requests", percentiles = {0.5, 0.95, 0.99})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}