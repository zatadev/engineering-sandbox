package com.zatadev.userservice.service;

import com.zatadev.userservice.domain.dto.CreateUserRequest;
import com.zatadev.userservice.domain.dto.UserResponse;
import com.zatadev.userservice.domain.entity.User;
import com.zatadev.userservice.exception.ConflictException;
import com.zatadev.userservice.exception.ResourceNotFoundException;
import com.zatadev.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testUser = User.builder()
                .id(testId)
                .username("zatadev")
                .email("zata@dev.com")
                .password("password123")
                .role("USER")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated list of users")
        void shouldReturnPaginatedUsers() {
            PageRequest pageable = PageRequest.of(0, 20);
            Page<User> page = new PageImpl<>(List.of(testUser));
            when(userRepository.findAll(pageable)).thenReturn(page);

            Page<UserResponse> result = userService.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).username()).isEqualTo("zatadev");
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(testId)).thenReturn(Optional.of(testUser));

            UserResponse result = userService.findById(testId);

            assertThat(result.id()).isEqualTo(testId);
            assertThat(result.username()).isEqualTo("zatadev");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(testId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(testId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(testId.toString());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create user successfully")
        void shouldCreateUserSuccessfully() {
            CreateUserRequest request = new CreateUserRequest("zatadev", "zata@dev.com", "password123");
            when(userRepository.existsByUsername("zatadev")).thenReturn(false);
            when(userRepository.existsByEmail("zata@dev.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserResponse result = userService.create(request);

            assertThat(result.username()).isEqualTo("zatadev");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw ConflictException when username exists")
        void shouldThrowWhenUsernameExists() {
            CreateUserRequest request = new CreateUserRequest("zatadev", "zata@dev.com", "password123");
            when(userRepository.existsByUsername("zatadev")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("zatadev");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ConflictException when email exists")
        void shouldThrowWhenEmailExists() {
            CreateUserRequest request = new CreateUserRequest("zatadev", "zata@dev.com", "password123");
            when(userRepository.existsByUsername("zatadev")).thenReturn(false);
            when(userRepository.existsByEmail("zata@dev.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("zata@dev.com");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete user when exists")
        void shouldDeleteWhenExists() {
            when(userRepository.existsById(testId)).thenReturn(true);

            userService.delete(testId);

            verify(userRepository).deleteById(testId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.existsById(testId)).thenReturn(false);

            assertThatThrownBy(() -> userService.delete(testId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).deleteById(any());
        }
    }
}