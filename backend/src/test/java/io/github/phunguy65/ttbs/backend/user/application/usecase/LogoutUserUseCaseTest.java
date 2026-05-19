package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.LogoutUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository.RefreshTokenData;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@DisplayName("LogoutUserUseCase")
class LogoutUserUseCaseTest {

    private static final String RAW_TOKEN = "raw-refresh-token";
    private static final String TOKEN_HASH = "hashed-refresh-token";
    private static final UUID TOKEN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UserId USER_ID =
            UserId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenManager refreshTokenManager;
    private LogoutUserUseCase useCase;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        refreshTokenManager = mock(RefreshTokenManager.class);
        useCase = new LogoutUserUseCase(refreshTokenRepository, refreshTokenManager);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("hashes, finds and revokes an active refresh token")
        void execute_revokesActiveRefreshToken() {
            RefreshTokenData tokenData = activeTokenData();
            when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(tokenData));

            Result<Void, UserError> result = useCase.execute(new LogoutUserCommand(RAW_TOKEN));

            assertThat(result).isInstanceOf(Result.Success.class);
            verify(refreshTokenManager).hashToken(RAW_TOKEN);
            verify(refreshTokenRepository).findActiveByTokenHash(TOKEN_HASH);
            verify(refreshTokenRepository).revokeById(TOKEN_ID);
        }

        @Test
        @DisplayName("executes hash, lookup and revoke in order")
        void execute_revokesInExpectedOrder() {
            when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(activeTokenData()));

            useCase.execute(new LogoutUserCommand(RAW_TOKEN));

            InOrder inOrder = inOrder(refreshTokenManager, refreshTokenRepository);
            inOrder.verify(refreshTokenManager).hashToken(RAW_TOKEN);
            inOrder.verify(refreshTokenRepository).findActiveByTokenHash(TOKEN_HASH);
            inOrder.verify(refreshTokenRepository).revokeById(TOKEN_ID);
        }
    }

    @Nested
    @DisplayName("idempotent behavior")
    class IdempotentBehavior {

        @Test
        @DisplayName("returns success and does not revoke when token is not active")
        void execute_returnsSuccessWhenTokenNotFound() {
            when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.empty());

            Result<Void, UserError> result = useCase.execute(new LogoutUserCommand(RAW_TOKEN));

            assertThat(result.isSuccess()).isTrue();
            verify(refreshTokenRepository, never()).revokeById(TOKEN_ID);
        }

        @Test
        @DisplayName("returns success regardless of token existence")
        void execute_alwaysReturnsSuccessForTokenExistence() {
            when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.empty());

            Result<Void, UserError> missingResult =
                    useCase.execute(new LogoutUserCommand(RAW_TOKEN));

            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(activeTokenData()));

            Result<Void, UserError> foundResult = useCase.execute(new LogoutUserCommand(RAW_TOKEN));

            assertThat(missingResult.isSuccess()).isTrue();
            assertThat(foundResult.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("passes the exact raw command token to the token manager")
        void execute_hashesExactRawToken() {
            String exactToken = "exact.raw.token";
            when(refreshTokenManager.hashToken(exactToken)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.empty());

            useCase.execute(new LogoutUserCommand(exactToken));

            verify(refreshTokenManager).hashToken(exactToken);
        }

        @Test
        @DisplayName("passes the exact hash returned by the token manager to the repository")
        void execute_usesExactTokenHash() {
            String exactHash = "exact-token-hash";
            when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(exactHash);
            when(refreshTokenRepository.findActiveByTokenHash(exactHash))
                    .thenReturn(Optional.empty());

            useCase.execute(new LogoutUserCommand(RAW_TOKEN));

            verify(refreshTokenRepository).findActiveByTokenHash(exactHash);
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates token hashing failures")
        void execute_propagatesHashTokenFailures() {
            when(refreshTokenManager.hashToken(RAW_TOKEN))
                    .thenThrow(new RuntimeException("hash failed"));

            assertThatThrownBy(() -> useCase.execute(new LogoutUserCommand(RAW_TOKEN)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("hash failed");
        }

        @Test
        @DisplayName("propagates active token lookup failures")
        void execute_propagatesFindActiveTokenFailures() {
            when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenThrow(new RuntimeException("lookup failed"));

            assertThatThrownBy(() -> useCase.execute(new LogoutUserCommand(RAW_TOKEN)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("lookup failed");
        }

        @Test
        @DisplayName("propagates revoke failures")
        void execute_propagatesRevokeFailures() {
            when(refreshTokenManager.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(activeTokenData()));
            when(refreshTokenRepository.findActiveByTokenHash(TOKEN_HASH))
                    .thenReturn(Optional.of(activeTokenData()));
            org.mockito.Mockito.doThrow(new RuntimeException("revoke failed"))
                    .when(refreshTokenRepository)
                    .revokeById(TOKEN_ID);

            assertThatThrownBy(() -> useCase.execute(new LogoutUserCommand(RAW_TOKEN)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("revoke failed");
        }
    }

    private RefreshTokenData activeTokenData() {
        return new RefreshTokenData(
                TOKEN_ID, USER_ID, TOKEN_HASH, Instant.parse("2026-05-16T10:15:30Z"));
    }
}
