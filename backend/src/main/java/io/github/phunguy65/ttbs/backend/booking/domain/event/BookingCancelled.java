package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.time.Instant;

public record BookingCancelled(
        BookingId bookingId,
        UserId userId,
        RouteId routeId,
        boolean requiresRefund,
        Instant occurredAt)
        implements DomainEvent {

    public BookingCancelled(
            BookingId bookingId, UserId userId, RouteId routeId, boolean requiresRefund) {
        this(bookingId, userId, routeId, requiresRefund, Instant.now());
    }
}
