package io.github.phunguy65.ttbs.backend.payment.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Objects;

public record CheckoutSessionId(String value) implements ValueObject {

    public CheckoutSessionId {
        Objects.requireNonNull(value, "checkoutSessionId must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("checkoutSessionId must not be blank");
        }
    }

    public static CheckoutSessionId of(String value) {
        return new CheckoutSessionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
