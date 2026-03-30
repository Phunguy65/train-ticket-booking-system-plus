package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
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

interface ScheduledTripJpaRepository extends JpaRepository<ScheduledTripEntity, UUID> {

    @Query("SELECT e FROM ScheduledTripEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<ScheduledTripEntity> findActiveById(@Param("id") UUID id);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary(
                e.id,
                e.routeTemplateId,
                e.trainId,
                e.departureTime,
                e.arrivalTime,
                e.status,
                e.createdAt
            ) FROM ScheduledTripEntity e WHERE e.id = :id AND e.deletedAt IS NULL
            """)
    Optional<ScheduledTripSummary> findSummaryById(@Param("id") UUID id);

    @Query("SELECT e FROM ScheduledTripEntity e WHERE e.deletedAt IS NULL")
    Page<ScheduledTripEntity> findAllActive(Pageable pageable);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary(
                e.id,
                e.routeTemplateId,
                e.trainId,
                e.departureTime,
                e.arrivalTime,
                e.status,
                e.createdAt
            ) FROM ScheduledTripEntity e WHERE e.deletedAt IS NULL
            """)
    Page<ScheduledTripSummary> findAllSummaries(Pageable pageable);

    @Query(
            "SELECT COUNT(e) > 0 FROM ScheduledTripEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") UUID id);

    @Query(
            "SELECT e.id FROM ScheduledTripEntity e WHERE e.trainId IN :trainIds AND e.deletedAt IS NULL")
    List<UUID> findActiveIdsByTrainIds(@Param("trainIds") List<UUID> trainIds);

    @Query(
            "SELECT e.id FROM ScheduledTripEntity e WHERE e.routeTemplateId = :routeTemplateId AND e.deletedAt IS NULL")
    List<UUID> findActiveIdsByRouteTemplateId(@Param("routeTemplateId") UUID routeTemplateId);

    @Modifying
    @Query(
            "UPDATE ScheduledTripEntity e SET e.deletedAt = :deletedAt WHERE e.id = :id AND e.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE ScheduledTripEntity e SET e.deletedAt = :deletedAt WHERE e.id IN :ids AND e.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}
