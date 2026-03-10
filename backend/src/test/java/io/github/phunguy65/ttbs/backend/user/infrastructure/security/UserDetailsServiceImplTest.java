package io.github.phunguy65.ttbs.backend.user.infrastructure.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void loadUserByUsername_activeUser_shouldReturnUserDetails() {
        UserId id = UserId.of(UUID.randomUUID());
        User user = User.reconstitute(
                id,
                "alice@example.com",
                "$2a$12$hash",
                "Alice",
                "090",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        var userDetails = service.loadUserByUsername(id.value().toString());

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(id.value().toString());
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    void loadUserByUsername_softDeletedUser_shouldThrowUsernameNotFoundException() {
        // findById (which uses findActiveById under the hood) returns empty for deleted users
        UserId id = UserId.of(UUID.randomUUID());
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(id.value().toString()))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(id.value().toString());
    }

    @Test
    void loadUserByUsername_nonExistentUser_shouldThrowUsernameNotFoundException() {
        UserId id = UserId.of(UUID.randomUUID());
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(id.value().toString()))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
