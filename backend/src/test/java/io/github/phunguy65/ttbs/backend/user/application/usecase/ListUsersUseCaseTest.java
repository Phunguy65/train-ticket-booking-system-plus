package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.user.application.query.GetUsersQuery;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListUsersUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListUsersUseCase listUsersUseCase;

    // ── Happy path ───────────────────────────────────────────────────────────

    @Test
    void execute_returnsPageResultOfUserDtos() {
        User user = User.reconstitute(
                UserId.of(UUID.randomUUID()),
                "alice@example.com",
                "$2a$12$hash",
                "Alice",
                "090",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);

        PageResponse<User> repoResult = PageResponse.of(List.of(user), 0, 20, false);
        when(userRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(repoResult);

        PageResponse<UserResponse> result = listUsersUseCase.execute(new GetUsersQuery(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).email()).isEqualTo("alice@example.com");
        assertThat(result.content().get(0).fullName()).isEqualTo("Alice");
        assertThat(result.content().get(0).role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void execute_hasNext_propagatedToDto() {
        User user = User.reconstitute(
                UserId.of(UUID.randomUUID()),
                "bob@example.com",
                "$2a$12$hash",
                "Bob",
                null,
                UserRole.ADMIN,
                Instant.now(),
                Instant.now(),
                null);

        PageResponse<User> repoResult = PageResponse.of(List.of(user), 0, 5, true);
        when(userRepository.findAll(eq(0), eq(5), any(List.class))).thenReturn(repoResult);

        PageResponse<UserResponse> result = listUsersUseCase.execute(new GetUsersQuery(0, 5));

        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void execute_middlePage_hasPreviousTrue() {
        PageResponse<User> repoResult = PageResponse.of(List.of(), 2, 10, false);
        when(userRepository.findAll(eq(2), eq(10), any(List.class))).thenReturn(repoResult);

        PageResponse<UserResponse> result = listUsersUseCase.execute(new GetUsersQuery(2, 10));

        assertThat(result.hasPrevious()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.page()).isEqualTo(2);
    }

    // ── Empty result ─────────────────────────────────────────────────────────

    @Test
    void execute_emptyDatabase_returnsEmptyPageResult() {
        PageResponse<User> repoResult = PageResponse.empty(20);
        when(userRepository.findAll(eq(0), eq(20), any(List.class))).thenReturn(repoResult);

        PageResponse<UserResponse> result = listUsersUseCase.execute(new GetUsersQuery(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    // ── No sensitive data exposed ────────────────────────────────────────────

    @Test
    void execute_userDto_doesNotContainPasswordHash() {
        User user = User.reconstitute(
                UserId.of(UUID.randomUUID()),
                "carol@example.com",
                "$2a$12$verysecretpasswordhash",
                "Carol",
                null,
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);

        PageResponse<User> repoResult = PageResponse.of(List.of(user), 0, 20, false);
        when(userRepository.findAll(anyInt(), anyInt(), any(List.class))).thenReturn(repoResult);

        PageResponse<UserResponse> result = listUsersUseCase.execute(new GetUsersQuery(0, 20));

        // UserResponse record does not have a passwordHash field – compile-time guarantee.
        // This test asserts the email is present and the dto is well-formed.
        UserResponse dto = result.content().get(0);
        assertThat(dto.email()).isEqualTo("carol@example.com");
        assertThat(dto.id()).isNotNull();
        assertThat(dto.createdAt()).isNotNull();
    }
}
