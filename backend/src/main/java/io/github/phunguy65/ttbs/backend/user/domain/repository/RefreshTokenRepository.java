package io.github.phunguy65.ttbs.backend.user.domain.repository;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for refresh token persistence.
 *
 * <p>Refresh tokens are an infrastructure concept (not a domain aggregate), but operations on
 * them are orchestrated by application-layer use cases via this port.
 */
public interface RefreshTokenRepository {

    /**
     * Persists a new refresh token.
     *
     * @param id         generated token ID (UUIDv7)
     * @param userId     owning user
     * @param tokenHash  BCrypt hash of the raw token string
     * @param expiresAt  when this token expires
     */
    void save(UUID id, UserId userId, String tokenHash, Instant expiresAt);

    /**
     * Finds an active (non-revoked, non-expired) refresh token by its hash.
     */
    Optional<RefreshTokenData> findActiveByTokenHash(String tokenHash);

    /**
     * Revokes a single token by its database ID.
     */
    void revokeById(UUID tokenId);

    /**
     * Revokes all active tokens belonging to a user (used on token-reuse detection).
     */
    void revokeAllByUserId(UserId userId);

    /**
     * Lightweight projection returned from lookups.
     */
    record RefreshTokenData(UUID id, UserId userId, String tokenHash, Instant expiresAt) {}
}
