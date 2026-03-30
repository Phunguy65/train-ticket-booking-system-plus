package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CoachJpaRepository extends JpaRepository<CoachEntity, UUID> {

    @Query("SELECT c FROM CoachEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CoachEntity> findActiveById(@Param("id") UUID id);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSummary(
                c.id,
                c.trainId,
                c.carNumber,
                c.totalSeats,
                c.createdAt
            ) FROM CoachEntity c WHERE c.id = :id AND c.deletedAt IS NULL
            """)
    Optional<CoachSummary> findSummaryById(@Param("id") UUID id);

    @Query(
            "SELECT c FROM CoachEntity c WHERE c.trainId = :trainId AND c.deletedAt IS NULL ORDER BY c.carNumber ASC")
    List<CoachEntity> findAllActiveByTrainId(@Param("trainId") UUID trainId);

    @Query("SELECT c FROM CoachEntity c WHERE c.trainId = :trainId AND c.deletedAt IS NULL")
    Page<CoachEntity> findAllActiveByTrainId(@Param("trainId") UUID trainId, Pageable pageable);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSummary(
                c.id,
                c.trainId,
                c.carNumber,
                c.totalSeats,
                c.createdAt
            ) FROM CoachEntity c WHERE c.trainId = :trainId AND c.deletedAt IS NULL
            """)
    Page<CoachSummary> findAllSummariesByTrainId(@Param("trainId") UUID trainId, Pageable pageable);

    boolean existsByTrainIdAndCarNumberAndDeletedAtIsNull(UUID trainId, Integer carNumber);

    @Query("SELECT c.id FROM CoachEntity c WHERE c.trainId IN :trainIds AND c.deletedAt IS NULL")
    List<UUID> findActiveIdsByTrainIds(@Param("trainIds") List<UUID> trainIds);

    @Modifying
    @Query(
            "UPDATE CoachEntity c SET c.deletedAt = :deletedAt WHERE c.id = :id AND c.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE CoachEntity c SET c.deletedAt = :deletedAt WHERE c.id IN :ids AND c.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}
