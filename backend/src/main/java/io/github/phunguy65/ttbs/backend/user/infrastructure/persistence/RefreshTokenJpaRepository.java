package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Query(
            "SELECT t FROM RefreshTokenEntity t WHERE t.tokenHash = :tokenHash AND t.revokedAt IS NULL")
    Optional<RefreshTokenEntity> findActiveByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query(
            "UPDATE RefreshTokenEntity t SET t.revokedAt = :revokedAt WHERE t.userId = :userId AND t.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
