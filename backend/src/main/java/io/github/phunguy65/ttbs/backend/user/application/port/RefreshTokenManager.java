package io.github.phunguy65.ttbs.backend.user.application.port;

import io.github.phunguy65.ttbs.backend.user.domain.model.User;

/**
 * Port for refresh token lifecycle management.
 * Implemented by {@code JwtRefreshTokenManager} in the security infrastructure layer.
 */
public interface RefreshTokenManager {

    /**
     * Generates a new access token and refresh token for the given user, persists the refresh
     * token hash, and returns the raw token pair.
     */
    TokenPair generateAndSaveTokens(User user);

    /**
     * Computes a hex-encoded SHA-256 hash of the given token string for secure storage and
     * lookup.
     */
    String hashToken(String token);

    record TokenPair(String accessToken, String refreshToken) {}
}
