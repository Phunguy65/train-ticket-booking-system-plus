package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(UUID id, UserId userId, String tokenHash, Instant expiresAt) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(id);
        entity.setUserId(userId.value());
        entity.setTokenHash(tokenHash);
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(Instant.now());
        jpaRepository.save(entity);
    }

    @Override
    public Optional<RefreshTokenData> findActiveByTokenHash(String tokenHash) {
        return jpaRepository
                .findActiveByTokenHash(tokenHash)
                .map(e -> new RefreshTokenData(
                        e.getId(), UserId.of(e.getUserId()), e.getTokenHash(), e.getExpiresAt()));
    }

    @Override
    public void revokeById(UUID tokenId) {
        jpaRepository.findById(tokenId).ifPresent(e -> {
            e.setRevokedAt(Instant.now());
            jpaRepository.save(e);
        });
    }

    @Override
    public void revokeAllByUserId(UserId userId) {
        jpaRepository.revokeAllByUserId(userId.value(), Instant.now());
    }
}
