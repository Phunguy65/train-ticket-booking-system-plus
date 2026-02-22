package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;

public record BookingConfirmed(BookingId bookingId, Instant occurredAt) implements DomainEvent {

    public static BookingConfirmed of(BookingId bookingId) {
        return new BookingConfirmed(bookingId, Instant.now());
    }
}
