package com.zatadev.userservice.repository;

import com.zatadev.userservice.AbstractIntegrationTest;
import com.zatadev.userservice.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRepository")
class UserRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private User buildUser(String username, String email) {
        return User.builder()
                .username(username)
                .email(email)
                .password("password123")
                .role("USER")
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("findByUsername")
    class FindByUsername {

        @Test
        @DisplayName("should find user by username")
        void shouldFindByUsername() {
            userRepository.save(buildUser("zatadev", "zata@dev.com"));

            var result = userRepository.findByUsername("zatadev");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("zata@dev.com");
        }

        @Test
        @DisplayName("should return empty when username not found")
        void shouldReturnEmptyWhenNotFound() {
            var result = userRepository.findByUsername("unknown");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUsername")
    class ExistsByUsername {

        @Test
        @DisplayName("should return true when username exists")
        void shouldReturnTrueWhenExists() {
            userRepository.save(buildUser("zatadev", "zata@dev.com"));

            assertThat(userRepository.existsByUsername("zatadev")).isTrue();
        }

        @Test
        @DisplayName("should return false when username not found")
        void shouldReturnFalseWhenNotFound() {
            assertThat(userRepository.existsByUsername("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("should return true when email exists")
        void shouldReturnTrueWhenExists() {
            userRepository.save(buildUser("zatadev", "zata@dev.com"));

            assertThat(userRepository.existsByEmail("zata@dev.com")).isTrue();
        }

        @Test
        @DisplayName("should return false when email not found")
        void shouldReturnFalseWhenNotFound() {
            assertThat(userRepository.existsByEmail("unknown@dev.com")).isFalse();
        }
    }
}