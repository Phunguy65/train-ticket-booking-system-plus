package io.github.phunguy65.ttbs.backend.booking.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence contract for {@link Booking}.
 *
 * <p>No JPA or Spring framework types appear here.
 */
public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(BookingId bookingId);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    Optional<Booking> findActiveHoldByUserAndRoute(UserId userId, RouteId routeId);

    List<Booking> findExpiredHeldBookings(Instant now);

    List<Booking> saveAll(List<Booking> bookings);
}
