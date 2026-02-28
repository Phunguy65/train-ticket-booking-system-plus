package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CoachJpaRepository extends JpaRepository<CoachEntity, UUID> {

    @Query("SELECT c FROM CoachEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CoachEntity> findActiveById(@Param("id") UUID id);

    @Query(
            "SELECT c FROM CoachEntity c WHERE c.trainId = :trainId AND c.deletedAt IS NULL ORDER BY c.carNumber ASC")
    List<CoachEntity> findAllActiveByTrainId(@Param("trainId") UUID trainId);

    boolean existsByTrainIdAndCarNumberAndDeletedAtIsNull(UUID trainId, Integer carNumber);

    @Modifying
    @Query(
            "UPDATE CoachEntity c SET c.deletedAt = :deletedAt WHERE c.id = :id AND c.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE CoachEntity c SET c.deletedAt = :deletedAt WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}
