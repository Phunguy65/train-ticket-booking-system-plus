package io.github.phunguy65.ttbs.backend.payment.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Objects;

public record StripeEventId(String value) implements ValueObject {

    public StripeEventId {
        Objects.requireNonNull(value, "stripeEventId must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("stripeEventId must not be blank");
        }
    }

    public static StripeEventId of(String value) {
        return new StripeEventId(value);
    }

    public static StripeEventId ofNullable(String value) {
        return value == null || value.isBlank() ? null : of(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
