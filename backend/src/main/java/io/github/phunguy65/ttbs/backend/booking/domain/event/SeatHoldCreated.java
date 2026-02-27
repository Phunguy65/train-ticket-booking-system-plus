package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published when a seat hold is successfully created.
 * Consumers can use this to display hold confirmation and countdown timers.
 */
public record SeatHoldCreated(
        BookingId bookingId,
        UUID userId,
        UUID routeId,
        List<UUID> seatIds,
        Instant expiresAt,
        Instant occurredAt)
        implements DomainEvent {

    public static SeatHoldCreated of(
            BookingId bookingId, UUID userId, UUID routeId, List<UUID> seatIds, Instant expiresAt) {
        return new SeatHoldCreated(bookingId, userId, routeId, seatIds, expiresAt, Instant.now());
    }
}
