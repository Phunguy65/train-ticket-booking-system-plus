package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository.RefreshTokenData;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUserUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenManager refreshTokenManager;

    public LogoutUserUseCase(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenManager refreshTokenManager) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenManager = refreshTokenManager;
    }

    /**
     * Revokes the given refresh token. Idempotent — if the token is not found or already
     * revoked, returns success anyway (no error to expose token existence).
     */
    @Transactional
    public Result<Void, UserError> execute(String rawRefreshToken) {
        String tokenHash = refreshTokenManager.hashToken(rawRefreshToken);
        Optional<RefreshTokenData> tokenData =
                refreshTokenRepository.findActiveByTokenHash(tokenHash);
        tokenData.ifPresent(t -> refreshTokenRepository.revokeById(t.id()));
        return Result.success();
    }
}
