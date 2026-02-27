package io.github.phunguy65.ttbs.backend.booking.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
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
     * Finds expired holds — bookings with status HELD and paymentDeadline before {@code now}.
     * Results are ordered by paymentDeadline ascending (oldest expiry first).
     *
     * @param now   the reference time for expiry check
     * @param limit maximum number of results to return
     * @return list of expired holds (may be empty)
     */
    List<Booking> findExpiredHolds(Instant now, int limit);

    /**
     * Finds a booking by ID, eagerly loading its booked seats.
     *
     * @return the booking with seats loaded, or empty if not found
     */
    Optional<Booking> findByIdWithSeats(BookingId id);
}
