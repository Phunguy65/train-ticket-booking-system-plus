package io.github.phunguy65.ttbs.backend.payment.domain.event;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmed(PaymentId paymentId, UUID bookingId, Instant occurredAt)
        implements DomainEvent {

    public static PaymentConfirmed of(PaymentId paymentId, UUID bookingId) {
        return new PaymentConfirmed(paymentId, bookingId, Instant.now());
    }
}
