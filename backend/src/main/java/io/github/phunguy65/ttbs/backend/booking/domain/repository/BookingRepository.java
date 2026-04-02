package io.github.phunguy65.ttbs.backend.booking.domain.repository;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
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

    Optional<BookingSummary> findSummaryById(BookingId bookingId);

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    PageResponse<BookingSummary> findByUserId(
            UserId userId, int page, int size, List<SortOrder> sort);

    Optional<Booking> findActiveHoldByUserAndScheduledTrip(
            UserId userId, ScheduledTripId scheduledTripId);

    List<Booking> findExpiredHeldBookings(Instant now);

    void cancelByIds(List<BookingId> bookingIds);

    List<Booking> saveAll(List<Booking> bookings);

    boolean existsActiveByUserId(UserId userId);
}
