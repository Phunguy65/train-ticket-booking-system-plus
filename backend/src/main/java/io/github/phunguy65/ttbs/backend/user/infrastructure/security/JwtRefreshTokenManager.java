package io.github.phunguy65.ttbs.backend.user.infrastructure.security;

import io.github.phunguy65.ttbs.backend.user.application.port.RefreshTokenManager;
import io.github.phunguy65.ttbs.backend.user.application.port.TokenProvider;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtRefreshTokenManager implements RefreshTokenManager {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    @Value("${jwt.refresh-token-expiry:604800}")
    private long refreshTokenExpirySeconds;

    public JwtRefreshTokenManager(
            RefreshTokenRepository refreshTokenRepository, TokenProvider tokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public TokenPair generateAndSaveTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String rawRefreshToken = tokenProvider.generateRefreshToken();

        String tokenHash = hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plus(refreshTokenExpirySeconds, ChronoUnit.SECONDS);
        refreshTokenRepository.save(UUID.randomUUID(), user.getId(), tokenHash, expiresAt);

        return new TokenPair(accessToken, rawRefreshToken);
    }

    @Override
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
