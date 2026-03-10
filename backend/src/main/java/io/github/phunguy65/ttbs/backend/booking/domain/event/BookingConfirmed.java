package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;

public record BookingConfirmed(
        BookingId bookingId, UserId userId, RouteId routeId, Instant occurredAt)
        implements DomainEvent {

    public BookingConfirmed(BookingId bookingId, UserId userId, RouteId routeId) {
        this(bookingId, userId, routeId, Instant.now());
    }
}
