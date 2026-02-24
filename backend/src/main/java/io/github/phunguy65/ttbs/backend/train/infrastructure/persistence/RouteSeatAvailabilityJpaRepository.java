package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RouteSeatAvailabilityJpaRepository
        extends JpaRepository<RouteSeatAvailabilityEntity, RouteSeatAvailabilityId> {

    @Query(
            "SELECT e FROM RouteSeatAvailabilityEntity e WHERE e.id.routeId = :routeId AND e.status = 'AVAILABLE'")
    List<RouteSeatAvailabilityEntity> findAvailableByRouteId(
            @Param("routeId") java.util.UUID routeId);

    @Query(
            "SELECT e FROM RouteSeatAvailabilityEntity e WHERE e.id.routeId = :routeId AND e.id.seatId = :seatId")
    Optional<RouteSeatAvailabilityEntity> findByRouteIdAndSeatId(
            @Param("routeId") java.util.UUID routeId, @Param("seatId") java.util.UUID seatId);
}
