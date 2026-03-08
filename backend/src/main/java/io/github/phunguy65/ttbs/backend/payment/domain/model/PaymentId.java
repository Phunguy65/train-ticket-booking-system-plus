package io.github.phunguy65.ttbs.backend.payment.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/** Type-safe UUID wrapper for a payment's primary key. */
public record PaymentId(UUID value) {

    public PaymentId {
        if (value == null) {
            throw new IllegalArgumentException("PaymentId value must not be null");
        }
    }

    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
