package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

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

interface RouteJpaRepository extends JpaRepository<RouteEntity, UUID> {

    @Query("SELECT r FROM RouteEntity r WHERE r.deletedAt IS NULL")
    Page<RouteEntity> findAllActive(Pageable pageable);

    @Query("SELECT r FROM RouteEntity r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<RouteEntity> findActiveById(@Param("id") UUID id);

    @Query(
            "SELECT COUNT(r) > 0 FROM RouteEntity r WHERE r.trainId = :trainId AND r.deletedAt IS NULL")
    boolean existsActiveRouteByTrainId(@Param("trainId") UUID trainId);

    @Query(
            "SELECT COUNT(r) > 0 FROM RouteEntity r WHERE (r.originStationId = :stationId OR r.destinationStationId = :stationId) AND r.deletedAt IS NULL")
    boolean existsActiveRouteByStationId(@Param("stationId") UUID stationId);

    @Query("SELECT COUNT(r) > 0 FROM RouteEntity r WHERE r.id = :id AND r.deletedAt IS NULL")
    boolean existsActiveById(@Param("id") UUID id);

    @Modifying
    @Query(
            "UPDATE RouteEntity r SET r.deletedAt = :deletedAt WHERE r.id = :id AND r.deletedAt IS NULL")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

    @Modifying
    @Query(
            "UPDATE RouteEntity r SET r.deletedAt = :deletedAt WHERE r.id IN :ids AND r.deletedAt IS NULL")
    int softDeleteByIds(@Param("ids") List<UUID> ids, @Param("deletedAt") Instant deletedAt);

    @Query(
            "SELECT r.id FROM RouteEntity r WHERE (r.originStationId = :stationId OR r.destinationStationId = :stationId) AND r.deletedAt IS NULL")
    List<UUID> findActiveIdsByStationId(@Param("stationId") UUID stationId);

    @Query(
            "SELECT r.id FROM RouteEntity r WHERE (r.originStationId IN :stationIds OR r.destinationStationId IN :stationIds) AND r.deletedAt IS NULL")
    List<UUID> findActiveIdsByStationIds(@Param("stationIds") List<UUID> stationIds);

    @Query(
            "SELECT DISTINCT r.trainId FROM RouteEntity r WHERE r.id IN :routeIds AND r.deletedAt IS NULL")
    List<UUID> findDistinctActiveTrainIdsByRouteIds(@Param("routeIds") List<UUID> routeIds);

    @Query("SELECT COUNT(r) FROM RouteEntity r WHERE r.trainId = :trainId AND r.deletedAt IS NULL")
    long countActiveByTrainId(@Param("trainId") UUID trainId);
}
