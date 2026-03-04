package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
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

    /**
     * Acquires pessimistic write locks on the specified seats for a given route.
     * Seats are returned in ascending seatId order to prevent deadlocks.
     * Lock timeout is set to 3000 ms — callers should handle LockTimeoutException.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("SELECT e FROM RouteSeatAvailabilityEntity e "
            + "WHERE e.id.routeId = :routeId AND e.id.seatId IN :seatIds "
            + "ORDER BY e.id.seatId ASC")
    List<RouteSeatAvailabilityEntity> findByRouteIdAndSeatIdsForUpdate(
            @Param("routeId") java.util.UUID routeId,
            @Param("seatIds") List<java.util.UUID> seatIds);

    @Query(
            "SELECT COUNT(e) > 0 FROM RouteSeatAvailabilityEntity e WHERE e.id.seatId = :seatId AND e.status IN ('HELD', 'BOOKED')")
    boolean existsActiveBySeatId(@Param("seatId") java.util.UUID seatId);

    @Query(
            "SELECT COUNT(e) > 0 FROM RouteSeatAvailabilityEntity e WHERE e.id.seatId IN :seatIds AND e.status IN ('HELD', 'BOOKED')")
    boolean existsActiveByAnyOfSeatIds(@Param("seatIds") List<java.util.UUID> seatIds);

    @Modifying
    @Query(
            value = "DELETE FROM route_seat_availability WHERE route_id IN :routeIds",
            nativeQuery = true)
    void hardDeleteByRouteIds(@Param("routeIds") List<UUID> routeIds);

    @Modifying
    @Query(
            value = "DELETE FROM route_seat_availability WHERE seat_id IN :seatIds",
            nativeQuery = true)
    void hardDeleteBySeatIds(@Param("seatIds") List<UUID> seatIds);
}
