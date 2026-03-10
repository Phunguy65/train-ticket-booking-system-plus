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
     * Fetches the specified seats for a given route without locking.
     * Optimistic locking via {@code @Version} handles concurrent modification detection.
     */
    List<RouteSeatAvailability> findByRouteIdAndSeatIds(RouteId routeId, List<SeatId> seatIds);

    List<RouteSeatAvailability> findByBookingId(java.util.UUID bookingId);

    List<RouteSeatAvailability> saveAll(List<RouteSeatAvailability> records);

    RouteSeatAvailability save(RouteSeatAvailability record);

    boolean existsActiveBySeatId(SeatId seatId);

    boolean existsActiveByAnyOfSeatIds(List<SeatId> seatIds);

    void hardDeleteByRouteIds(List<RouteId> routeIds);

    void hardDeleteBySeatIds(List<SeatId> seatIds);
}
