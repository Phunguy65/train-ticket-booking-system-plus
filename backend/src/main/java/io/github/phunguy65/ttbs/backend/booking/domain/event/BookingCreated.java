package io.github.phunguy65.ttbs.backend.booking.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;

public record BookingCreated(
        BookingId bookingId,
        UserId userId,
        ScheduledTripId scheduledTripId,
        Money totalPrice,
        String currency,
        Instant occurredAt)
        implements DomainEvent {

    public BookingCreated(
            BookingId bookingId,
            UserId userId,
            ScheduledTripId scheduledTripId,
            Money totalPrice,
            String currency) {
        this(bookingId, userId, scheduledTripId, totalPrice, currency, Instant.now());
    }
}
