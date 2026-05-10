package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.LoginCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUserUseCase")
class LoginUserUseCaseTest {

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "secret";
    private static final String HASHED_PASSWORD = "hashed-secret";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenManager refreshTokenManager;

    private LoginUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUserUseCase(userRepository, passwordEncoder, refreshTokenManager);
    }

    private User reconstitutedUser() {
        return User.reconstitute(
                UserId.of(UUID.randomUUID()),
                EmailAddress.of(EMAIL),
                PasswordHash.of(HASHED_PASSWORD),
                PersonName.of("Nguyen Van A"),
                null,
                null,
                null,
                null,
                null,
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName(
                "finds user by email, verifies password, generates tokens and returns LoginResultResponse")
        void execute_returnsLoginResultResponse() {
            User user = reconstitutedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(refreshTokenManager.generateAndSaveTokens(user))
                    .thenReturn(new RefreshTokenManager.TokenPair("access-token", "refresh-token"));

            Result<LoginResultResponse, UserError> result =
                    useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            assertThat(result.isSuccess()).isTrue();
            LoginResultResponse response =
                    ((Result.Success<LoginResultResponse, UserError>) result).value();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user().email()).isEqualTo(EMAIL);
        }
    }

    @Nested
    @DisplayName("invalid credentials")
    class InvalidCredentials {

        @Test
        @DisplayName("returns InvalidCredentials when email not found")
        void execute_returnsInvalidCredentials_whenEmailNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            Result<LoginResultResponse, UserError> result =
                    useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, UserError>) result).error())
                    .isInstanceOf(UserError.InvalidCredentials.class);
        }

        @Test
        @DisplayName("returns InvalidCredentials when password wrong")
        void execute_returnsInvalidCredentials_whenPasswordWrong() {
            User user = reconstitutedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

            Result<LoginResultResponse, UserError> result =
                    useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<?, UserError>) result).error())
                    .isInstanceOf(UserError.InvalidCredentials.class);
        }
    }
}
