package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.projection.RouteTemplateSummary;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RouteTemplateJpaRepository extends JpaRepository<RouteTemplateEntity, UUID> {

    @Query("SELECT e FROM RouteTemplateEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<RouteTemplateEntity> findActiveById(@Param("id") UUID id);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.RouteTemplateSummary(
                e.id,
                e.originStationId,
                e.destinationStationId,
                e.basePrice,
                'VND',
                e.createdAt
            ) FROM RouteTemplateEntity e WHERE e.id = :id AND e.deletedAt IS NULL
            """)
    Optional<RouteTemplateSummary> findSummaryById(@Param("id") UUID id);

    @Query("SELECT e FROM RouteTemplateEntity e WHERE e.deletedAt IS NULL")
    Page<RouteTemplateEntity> findAllActive(Pageable pageable);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.train.domain.projection.RouteTemplateSummary(
                e.id,
                e.originStationId,
                e.destinationStationId,
                e.basePrice,
                'VND',
                e.createdAt
            ) FROM RouteTemplateEntity e WHERE e.deletedAt IS NULL
            """)
    Page<RouteTemplateSummary> findAllSummaries(Pageable pageable);

    @Query(
            "SELECT COUNT(e) > 0 FROM RouteTemplateEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") UUID id);

    @Query(
            "SELECT COUNT(e) > 0 FROM RouteTemplateEntity e WHERE (e.originStationId = :stationId OR e.destinationStationId = :stationId) AND e.deletedAt IS NULL")
    boolean existsActiveByStationId(@Param("stationId") UUID stationId);

    @Modifying
    @Query(
            "UPDATE RouteTemplateEntity e SET e.deletedAt = :deletedAt WHERE e.id = :id AND e.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);
}
