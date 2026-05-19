package io.github.phunguy65.ttbs.backend.station.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary;
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

interface StationJpaRepository extends JpaRepository<StationEntity, UUID> {

    boolean existsByCode(String code);

    @Query("SELECT s FROM StationEntity s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<StationEntity> findActiveById(@Param("id") UUID id);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary(
                s.id,
                s.code,
                s.name,
                s.city,
                s.createdAt
            ) FROM StationEntity s WHERE s.id = :id AND s.deletedAt IS NULL
            """)
    Optional<StationSummary> findSummaryById(@Param("id") UUID id);

    @Query("SELECT s FROM StationEntity s WHERE s.deletedAt IS NULL")
    Page<StationEntity> findAllActive(Pageable pageable);

    @Query("""
            SELECT new io.github.phunguy65.ttbs.backend.station.domain.projection.StationSummary(
                s.id,
                s.code,
                s.name,
                s.city,
                s.createdAt
            ) FROM StationEntity s WHERE s.deletedAt IS NULL
            """)
    Page<StationSummary> findAllSummaries(Pageable pageable);

    @Modifying
    @Query(
            "UPDATE StationEntity s SET s.deletedAt = :deletedAt WHERE s.id = :id AND s.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE StationEntity s SET s.deletedAt = :deletedAt WHERE s.id IN :ids AND s.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);
}
