package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published when a seat hold expires (payment deadline passed without confirmation).
 * Consumers can use this to notify the user and update UI state.
 */
public record SeatHoldExpired(
        BookingId bookingId, UUID userId, UUID routeId, List<UUID> seatIds, Instant occurredAt)
        implements DomainEvent {

    public static SeatHoldExpired of(
            BookingId bookingId, UUID userId, UUID routeId, List<UUID> seatIds) {
        return new SeatHoldExpired(bookingId, userId, routeId, seatIds, Instant.now());
    }
}
