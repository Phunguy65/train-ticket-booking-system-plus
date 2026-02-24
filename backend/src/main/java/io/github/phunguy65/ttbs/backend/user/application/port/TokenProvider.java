package io.github.phunguy65.ttbs.backend.user.application.port;

import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;

/**
 * Port for JWT token generation and validation.
 * Implemented by {@code JwtTokenProvider} in the security infrastructure layer.
 */
public interface TokenProvider {

    /**
     * Generates a short-lived JWT access token for the given user.
     */
    String generateAccessToken(User user);

    /**
     * Generates a cryptographically random refresh token string (not a JWT).
     */
    String generateRefreshToken();

    /**
     * Returns true if the token is a valid, non-expired JWT signed by this provider.
     */
    boolean validateToken(String token);

    /**
     * Extracts the user ID claim from a valid access token.
     */
    UserId extractUserId(String token);
}
