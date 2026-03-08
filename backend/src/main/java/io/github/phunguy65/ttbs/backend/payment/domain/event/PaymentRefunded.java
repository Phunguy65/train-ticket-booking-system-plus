package io.github.phunguy65.ttbs.backend.payment.domain.event;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;

public record PaymentRefunded(PaymentId paymentId, BookingId bookingId, Instant occurredAt)
        implements DomainEvent {

    public PaymentRefunded(PaymentId paymentId, BookingId bookingId) {
        this(paymentId, bookingId, Instant.now());
    }
}
