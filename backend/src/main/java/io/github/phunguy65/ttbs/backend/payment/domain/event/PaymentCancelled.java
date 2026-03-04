package io.github.phunguy65.ttbs.backend.payment.domain.event;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PaymentCancelled(PaymentId paymentId, UUID bookingId, Instant occurredAt)
        implements DomainEvent {

    public static PaymentCancelled of(PaymentId paymentId, UUID bookingId) {
        return new PaymentCancelled(paymentId, bookingId, Instant.now());
    }
}
