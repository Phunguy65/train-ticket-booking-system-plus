package io.github.phunguy65.ttbs.backend.payment.domain.event;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreated(PaymentId paymentId, UUID bookingId, Instant occurredAt)
        implements DomainEvent {

    public static PaymentCreated of(PaymentId paymentId, UUID bookingId) {
        return new PaymentCreated(paymentId, bookingId, Instant.now());
    }
}
