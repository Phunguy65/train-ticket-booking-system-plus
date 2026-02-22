package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository.RefreshTokenData;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutUserUseCaseTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenManager refreshTokenManager;

    private LogoutUserUseCase useCase;

    private static final UUID TOKEN_ID = UUID.randomUUID();
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final String RAW_TOKEN = "rawRefreshTokenForLogoutTest12345";

    @BeforeEach
    void setUp() {
        useCase = new LogoutUserUseCase(refreshTokenRepository, refreshTokenManager);
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
    void execute_activeToken_shouldRevokeAndReturnSuccess() {
        String tokenHash = sha256(RAW_TOKEN);
        RefreshTokenData tokenData =
                new RefreshTokenData(TOKEN_ID, USER_ID, tokenHash, Instant.now().plusSeconds(3600));
        when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(tokenHash);
        when(refreshTokenRepository.findActiveByTokenHash(tokenHash))
                .thenReturn(Optional.of(tokenData));

        Result<Void, UserError> result = useCase.execute(RAW_TOKEN);

        assertThat(result.isSuccess()).isTrue();
        verify(refreshTokenRepository).revokeById(TOKEN_ID);
    }

    @Test
    void execute_alreadyRevoked_shouldReturnSuccessIdempotently() {
        String tokenHash = sha256(RAW_TOKEN);
        when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(tokenHash);
        when(refreshTokenRepository.findActiveByTokenHash(tokenHash)).thenReturn(Optional.empty());

        Result<Void, UserError> result = useCase.execute(RAW_TOKEN);

        assertThat(result.isSuccess()).isTrue();
        verify(refreshTokenRepository, never()).revokeById(any());
    }
}
