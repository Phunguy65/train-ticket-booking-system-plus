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

    /**
     * Returns the booking UUID associated with the given seat when it is in an active state
     * (HELD or BOOKED). Returns an empty list if the seat has no active booking.
     */
    List<java.util.UUID> findActiveBookingIdsBySeatId(SeatId seatId);

    /**
     * Returns distinct booking UUIDs for all seats in the given list that are in an active state
     * (HELD or BOOKED). Used to identify bookings that must be cancelled before bulk seat deletion.
     */
    List<java.util.UUID> findDistinctActiveBookingIdsBySeatIds(List<SeatId> seatIds);

    void hardDeleteByRouteIds(List<RouteId> routeIds);

    void hardDeleteBySeatIds(List<SeatId> seatIds);
}
