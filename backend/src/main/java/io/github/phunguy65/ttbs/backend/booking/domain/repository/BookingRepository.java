package io.github.phunguy65.ttbs.backend.booking.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(BookingId id);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    /**
     * Finds an active (HELD) booking for the given user and route.
     *
     * @return the active hold if one exists, otherwise empty
     */
    Optional<Booking> findActiveHoldByUserIdAndRouteId(UserId userId, RouteId routeId);

    /**
     * Finds a booking by ID, eagerly loading its booked seats.
     *
     * @return the booking with seats loaded, or empty if not found
     */
    Optional<Booking> findByIdWithSeats(BookingId id);

    /**
     * Finds HELD bookings that have a checkoutSessionId and were created before {@code threshold}.
     * Used by the reconciliation job to detect stale holds whose webhooks may have been missed.
     */
    List<Booking> findStaleHoldsWithCheckoutSession(Instant threshold);

    /**
     * Finds a booking by its Stripe Checkout Session ID.
     */
    Optional<Booking> findByCheckoutSessionId(String checkoutSessionId);

    /**
     * Returns {@code true} if there are any non-cancelled bookings (HELD or CONFIRMED)
     * belonging to the given user.
     *
     * @param userId the user to check
     * @return {@code true} if deletion is blocked
     */
    boolean existsActiveByUserId(UserId userId);

    /**
     * Returns {@code true} if there are any historical bookings that reference the given seat.
     *
     * @param seatId the seat to check
     * @return {@code true} if deletion is blocked
     */
    boolean existsBySeatId(SeatId seatId);
}
