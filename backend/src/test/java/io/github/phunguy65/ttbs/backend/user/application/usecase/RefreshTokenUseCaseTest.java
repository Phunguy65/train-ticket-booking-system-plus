package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.RefreshTokenCommand;
import io.github.phunguy65.ttbs.backend.user.application.dto.LoginResultDto;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository.RefreshTokenData;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenManager refreshTokenManager;

    private RefreshTokenUseCase useCase;

    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final UUID TOKEN_ID = UUID.randomUUID();
    // A raw refresh token (base64-like string)
    private static final String RAW_TOKEN = "rawRefreshTokenStringForTesting12345";

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(
                refreshTokenRepository, userRepository, refreshTokenManager);
    }

    private User makeUser() {
        return User.reconstitute(
                USER_ID,
                "alice@example.com",
                "$2a$12$hash",
                "Alice",
                "090",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now(),
                null);
    }

    private String sha256(String input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void execute_success_shouldRotateTokenAndReturnNewTokens() {
        String tokenHash = sha256(RAW_TOKEN);
        Instant futureExpiry = Instant.now().plusSeconds(3600);
        RefreshTokenData tokenData =
                new RefreshTokenData(TOKEN_ID, USER_ID, tokenHash, futureExpiry);

        when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(tokenHash);
        when(refreshTokenRepository.findActiveByTokenHash(tokenHash))
                .thenReturn(Optional.of(tokenData));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(makeUser()));
        when(refreshTokenManager.generateAndSaveTokens(any()))
                .thenReturn(new RefreshTokenManager.TokenPair(
                        "new-access-token", "new-raw-refresh-token"));

        Result<LoginResultDto, UserError> result =
                useCase.execute(new RefreshTokenCommand(RAW_TOKEN));

        assertThat(result.isSuccess()).isTrue();
        LoginResultDto dto = ((Result.Success<LoginResultDto, UserError>) result).value();
        assertThat(dto.accessToken()).isEqualTo("new-access-token");
        assertThat(dto.refreshToken()).isEqualTo("new-raw-refresh-token");
        verify(refreshTokenRepository).revokeById(TOKEN_ID);
        verify(refreshTokenManager).generateAndSaveTokens(any());
    }

    @Test
    void execute_expiredToken_shouldReturnInvalidRefreshToken() {
        String tokenHash = sha256(RAW_TOKEN);
        Instant pastExpiry = Instant.now().minusSeconds(3600);
        RefreshTokenData expiredToken =
                new RefreshTokenData(TOKEN_ID, USER_ID, tokenHash, pastExpiry);

        when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(tokenHash);
        when(refreshTokenRepository.findActiveByTokenHash(tokenHash))
                .thenReturn(Optional.of(expiredToken));

        Result<LoginResultDto, UserError> result =
                useCase.execute(new RefreshTokenCommand(RAW_TOKEN));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<LoginResultDto, UserError>) result).error())
                .isInstanceOf(UserError.InvalidRefreshToken.class);
        verify(refreshTokenRepository).revokeById(TOKEN_ID);
    }

    @Test
    void execute_tokenNotFound_shouldReturnInvalidRefreshToken() {
        String tokenHash = sha256(RAW_TOKEN);
        when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(tokenHash);
        when(refreshTokenRepository.findActiveByTokenHash(tokenHash)).thenReturn(Optional.empty());

        Result<LoginResultDto, UserError> result =
                useCase.execute(new RefreshTokenCommand(RAW_TOKEN));

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<LoginResultDto, UserError>) result).error())
                .isInstanceOf(UserError.InvalidRefreshToken.class);
    }
}
