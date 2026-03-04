package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;

public record BookingCancelled(BookingId bookingId, String checkoutSessionId, Instant occurredAt)
        implements DomainEvent {

    public static BookingCancelled of(BookingId bookingId, String checkoutSessionId) {
        return new BookingCancelled(bookingId, checkoutSessionId, Instant.now());
    }
}
