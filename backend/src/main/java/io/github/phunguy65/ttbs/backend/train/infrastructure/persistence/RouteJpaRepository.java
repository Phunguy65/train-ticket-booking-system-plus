package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RouteJpaRepository extends JpaRepository<RouteEntity, UUID> {

    @Query("""
            SELECT r FROM RouteEntity r
            WHERE (:originStationId IS NULL OR r.originStationId = :originStationId)
              AND (:destinationStationId IS NULL OR r.destinationStationId = :destinationStationId)
              AND (:departureDateFrom IS NULL OR r.departureTime >= :departureDateFrom)
              AND (:departureDateTo IS NULL OR r.departureTime <= :departureDateTo)
            """)
    Slice<RouteEntity> findAllWithFilter(
            @Param("originStationId") UUID originStationId,
            @Param("destinationStationId") UUID destinationStationId,
            @Param("departureDateFrom") Instant departureDateFrom,
            @Param("departureDateTo") Instant departureDateTo,
            Pageable pageable);
}
