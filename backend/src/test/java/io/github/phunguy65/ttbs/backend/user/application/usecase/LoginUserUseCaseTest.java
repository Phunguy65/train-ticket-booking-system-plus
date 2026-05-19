package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
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
import java.time.LocalDate;
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
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant CREATED_AT = Instant.parse("2026-05-15T10:15:30Z");

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
                UserId.of(USER_ID),
                EmailAddress.of(EMAIL),
                PasswordHash.of(HASHED_PASSWORD),
                PersonName.of("Nguyen Van A"),
                PhoneNumber.of("+84901234567"),
                LocalDate.of(1995, 5, 15),
                io.github.phunguy65.ttbs.backend.shared.domain.Gender.of("female"),
                IdDocumentNumber.of("012345678901"),
                AddressLine.of("123 Test Street"),
                UserRole.CUSTOMER,
                CREATED_AT,
                CREATED_AT,
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

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("checks raw command password against the stored password hash")
        void execute_checksRawPasswordAgainstStoredHash() {
            User user = reconstitutedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(refreshTokenManager.generateAndSaveTokens(user))
                    .thenReturn(new RefreshTokenManager.TokenPair("access-token", "refresh-token"));

            useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            verify(passwordEncoder).matches(RAW_PASSWORD, HASHED_PASSWORD);
        }

        @Test
        @DisplayName("generates and saves tokens for the found user")
        void execute_generatesAndSavesTokensForFoundUser() {
            User user = reconstitutedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(refreshTokenManager.generateAndSaveTokens(user))
                    .thenReturn(new RefreshTokenManager.TokenPair("access-token", "refresh-token"));

            useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            verify(refreshTokenManager).generateAndSaveTokens(user);
        }

        @Test
        @DisplayName("does not generate tokens when password does not match")
        void execute_doesNotGenerateTokensWhenPasswordDoesNotMatch() {
            User user = reconstitutedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

            useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            verify(refreshTokenManager, never()).generateAndSaveTokens(any());
        }

        @Test
        @DisplayName("does not generate tokens when email is not found")
        void execute_doesNotGenerateTokensWhenEmailNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            verify(refreshTokenManager, never()).generateAndSaveTokens(any());
        }

        @Test
        @DisplayName("maps the token pair and user fields into the login response")
        void execute_mapsFullLoginResultResponse() {
            User user = reconstitutedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(refreshTokenManager.generateAndSaveTokens(user))
                    .thenReturn(new RefreshTokenManager.TokenPair("access-token", "refresh-token"));

            Result<LoginResultResponse, UserError> result =
                    useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD));

            LoginResultResponse response =
                    ((Result.Success<LoginResultResponse, UserError>) result).value();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user().id()).isEqualTo(USER_ID);
            assertThat(response.user().email()).isEqualTo(EMAIL);
            assertThat(response.user().fullName()).isEqualTo("Nguyen Van A");
            assertThat(response.user().role()).isEqualTo("CUSTOMER");
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates refresh token generation failures")
        void execute_propagatesRefreshTokenGenerationFailures() {
            User user = reconstitutedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(refreshTokenManager.generateAndSaveTokens(user))
                    .thenThrow(new RuntimeException("token store down"));

            assertThatThrownBy(() -> useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("token store down");
        }

        @Test
        @DisplayName("propagates user lookup failures")
        void execute_propagatesUserLookupFailures() {
            when(userRepository.findByEmail(EMAIL)).thenThrow(new RuntimeException("db down"));

            assertThatThrownBy(() -> useCase.execute(new LoginCommand(EMAIL, RAW_PASSWORD)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db down");
        }
    }
}
