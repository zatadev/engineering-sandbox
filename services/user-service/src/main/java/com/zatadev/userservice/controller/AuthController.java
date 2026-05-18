package com.zatadev.userservice.controller;

import com.zatadev.userservice.config.JwtService;
import com.zatadev.userservice.domain.dto.AuthResponse;
import com.zatadev.userservice.domain.dto.LoginRequest;
import com.zatadev.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * STUB — Basic authentication controller for development purposes only.
 * Will be replaced by Keycloak/OAuth2 flow in Phase 8.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(user -> user.getPassword().equals(request.password()))
                .map(user -> {
                    log.info("Successful login for user: {}", user.getUsername());
                    String token = jwtService.generateToken(user.getUsername());
                    return ResponseEntity.ok(AuthResponse.builder()
                            .accessToken(token)
                            .tokenType("Bearer")
                            .expiresIn(900)
                            .build());
                })
                .orElseGet(() -> {
                    log.warn("Failed login attempt for username: {}", request.username());
                    return ResponseEntity.status(401).build();
                });
    }
}