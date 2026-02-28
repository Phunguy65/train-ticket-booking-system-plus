package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence contract for {@link RouteSeatAvailability}.
 *
 * <p>No JPA or Spring framework types appear here.
 */
public interface RouteSeatAvailabilityRepository {

    List<RouteSeatAvailability> findAvailableByRouteId(RouteId routeId);

    Optional<RouteSeatAvailability> findByRouteIdAndSeatId(RouteId routeId, SeatId seatId);

    /**
     * Acquires pessimistic write locks on the specified seats for a given route.
     * Seat IDs MUST be sorted ascending before calling to prevent deadlocks.
     * Returns at most {@code seatIds.size()} records.
     */
    List<RouteSeatAvailability> findByRouteIdAndSeatIdsForUpdate(
            RouteId routeId, List<SeatId> seatIds);

    List<RouteSeatAvailability> saveAll(List<RouteSeatAvailability> records);

    RouteSeatAvailability save(RouteSeatAvailability record);

    boolean existsActiveBySeatId(SeatId seatId);

    boolean existsActiveByAnyOfSeatIds(List<SeatId> seatIds);
}
