package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.BookedSeatSummary;
import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence contract for {@link RouteSeatAvailability}.
 *
 * <p>No JPA or Spring framework types appear here.
 */
public interface RouteSeatAvailabilityRepository {

    List<RouteSeatAvailability> findAvailableByScheduledTripId(ScheduledTripId scheduledTripId);

    /**
     * Returns ALL seat availability records for a given scheduled trip, regardless of status.
     * Used to support SSE initial state and full seat map display.
     */
    List<RouteSeatAvailability> findAllByScheduledTripId(ScheduledTripId scheduledTripId);

    Optional<RouteSeatAvailability> findByScheduledTripIdAndSeatId(
            ScheduledTripId scheduledTripId, SeatId seatId);

    /**
     * Fetches the specified seats for a given route without locking.
     * Optimistic locking via {@code @Version} handles concurrent modification detection.
     */
    List<RouteSeatAvailability> findByScheduledTripIdAndSeatIds(
            ScheduledTripId scheduledTripId, List<SeatId> seatIds);

    List<RouteSeatAvailability> findByBookingId(BookingId bookingId);

    List<BookedSeatSummary> findBookedSeatSummariesByBookingId(BookingId bookingId);

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

    void hardDeleteByScheduledTripIds(List<ScheduledTripId> scheduledTripIds);

    void hardDeleteBySeatIds(List<SeatId> seatIds);
}
