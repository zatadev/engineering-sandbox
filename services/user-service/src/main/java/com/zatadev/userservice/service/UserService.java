package com.zatadev.userservice.service;

import com.zatadev.userservice.config.CacheConfig;
import com.zatadev.userservice.domain.dto.CreateUserRequest;
import com.zatadev.userservice.domain.dto.UpdateUserRequest;
import com.zatadev.userservice.domain.dto.UserResponse;
import com.zatadev.userservice.domain.entity.User;
import com.zatadev.userservice.exception.ConflictException;
import com.zatadev.userservice.exception.ResourceNotFoundException;
import com.zatadev.userservice.repository.UserRepository;
import io.micrometer.core.annotation.Counted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service managing user lifecycle operations.
 * <p>
 * Password hashing is intentionally omitted in this stub implementation
 * and will be handled by Keycloak in Phase 8.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Returns a paginated list of all users.
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * Returns a single user by ID.
     *
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.USERS_CACHE, key = "#id")
    public UserResponse findById(UUID id) {
        log.debug("Cache miss — fetching user {} from database", id);
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Creates a new user.
     *
     * @throws ConflictException if username or email already exists
     */
    @Counted(value = "users.registered", description = "Total number of users registered")
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(request.password()) // sera hashé en Phase 8
                .role("USER")
                .active(true)
                .build();

        return toResponse(userRepository.save(user));
    }

    /**
     * Updates username and email of an existing user.
     *
     * @throws ResourceNotFoundException if no user exists with the given ID
     * @throws ConflictException if the new username or email already exists
     */
    @Transactional
    @CacheEvict(value = CacheConfig.USERS_CACHE, key = "#id")
    public UserResponse update(UUID id, UpdateUserRequest request) {
        log.debug("Cache evict — invalidating user {} after update", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getUsername().equals(request.username())
                && userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists: " + request.username());
        }
        if (!user.getEmail().equals(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }

        user.setUsername(request.username());
        user.setEmail(request.email());

        return toResponse(userRepository.save(user));
    }

    /**
     * Deletes a user by ID.
     *
     * @throws ResourceNotFoundException if no user exists with the given ID
     */
    @Transactional
    @CacheEvict(value = CacheConfig.USERS_CACHE, key = "#id")
    public void delete(UUID id) {
        log.debug("Cache evict — invalidating user {} after delete", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}