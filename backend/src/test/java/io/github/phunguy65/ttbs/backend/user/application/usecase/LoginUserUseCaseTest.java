package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.LoginCommand;
import io.github.phunguy65.ttbs.backend.user.application.dto.LoginResultDto;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenManager refreshTokenManager;

    private LoginUserUseCase useCase;

    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final String EMAIL = "alice@example.com";
    private static final String CORRECT_PASSWORD = "correctPassword";
    private static final String PASSWORD_HASH = "$2a$12$hashedCorrectPassword";

    @BeforeEach
    void setUp() {
        useCase = new LoginUserUseCase(userRepository, passwordEncoder, refreshTokenManager);
    }

    private User makeUser() {
        return User.reconstitute(
                USER_ID,
                EMAIL,
                PASSWORD_HASH,
                "Alice",
                "090",
                io.github.phunguy65.ttbs.backend.user.domain.model.UserRole.CUSTOMER,
                java.time.Instant.now(),
                java.time.Instant.now());
    }

    @Test
    void execute_success_shouldReturnTokens() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(makeUser()));
        when(passwordEncoder.matches(CORRECT_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(refreshTokenManager.generateAndSaveTokens(any()))
                .thenReturn(new RefreshTokenManager.TokenPair("access-token", "raw-refresh-token"));

        Result<LoginResultDto, UserError> result =
                useCase.execute(new LoginCommand(EMAIL, CORRECT_PASSWORD));

        assertThat(result.isSuccess()).isTrue();
        LoginResultDto dto = ((Result.Success<LoginResultDto, UserError>) result).value();
        assertThat(dto.accessToken()).isEqualTo("access-token");
        assertThat(dto.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(dto.user().email()).isEqualTo(EMAIL);
        verify(refreshTokenManager).generateAndSaveTokens(any());
    }

    @Test
    void execute_wrongPassword_shouldReturnInvalidCredentials() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(makeUser()));
        when(passwordEncoder.matches("wrongPassword", PASSWORD_HASH)).thenReturn(false);

        Result<LoginResultDto, UserError> result =
                useCase.execute(new LoginCommand(EMAIL, "wrongPassword"));

        assertThat(result.isFailure()).isTrue();
        UserError error = ((Result.Failure<LoginResultDto, UserError>) result).error();
        assertThat(error).isInstanceOf(UserError.InvalidCredentials.class);
    }

    @Test
    void execute_unknownEmail_shouldReturnInvalidCredentials() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Result<LoginResultDto, UserError> result =
                useCase.execute(new LoginCommand("unknown@example.com", "anyPassword"));

        assertThat(result.isFailure()).isTrue();
        UserError error = ((Result.Failure<LoginResultDto, UserError>) result).error();
        assertThat(error).isInstanceOf(UserError.InvalidCredentials.class);
    }
}
