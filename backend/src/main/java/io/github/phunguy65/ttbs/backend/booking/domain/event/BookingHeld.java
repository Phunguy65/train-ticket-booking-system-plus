package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingHeld(
        BookingId bookingId, UUID userId, UUID routeId, List<UUID> seatIds, Instant occurredAt)
        implements DomainEvent {

    public static BookingHeld of(
            BookingId bookingId, UUID userId, UUID routeId, List<UUID> seatIds) {
        return new BookingHeld(bookingId, userId, routeId, seatIds, Instant.now());
    }
}
