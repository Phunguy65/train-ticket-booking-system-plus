package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record BookingCreated(
        BookingId bookingId, UUID userId, UUID routeId, UUID seatId, Instant occurredAt)
        implements DomainEvent {

    public static BookingCreated of(BookingId bookingId, UUID userId, UUID routeId, UUID seatId) {
        return new BookingCreated(bookingId, userId, routeId, seatId, Instant.now());
    }
}
