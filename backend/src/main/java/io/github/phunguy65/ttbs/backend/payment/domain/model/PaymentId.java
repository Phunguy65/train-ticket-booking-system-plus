package io.github.phunguy65.ttbs.backend.payment.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.UUID;

public record PaymentId(UUID value) implements ValueObject {

    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }

    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID());
    }
}
