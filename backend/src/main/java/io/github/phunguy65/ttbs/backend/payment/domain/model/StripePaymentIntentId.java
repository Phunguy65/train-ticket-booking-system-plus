package io.github.phunguy65.ttbs.backend.payment.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Objects;

public record StripePaymentIntentId(String value) implements ValueObject {

    public StripePaymentIntentId {
        Objects.requireNonNull(value, "stripePaymentIntentId must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("stripePaymentIntentId must not be blank");
        }
    }

    public static StripePaymentIntentId of(String value) {
        return new StripePaymentIntentId(value);
    }

    public static StripePaymentIntentId ofNullable(String value) {
        return value == null || value.isBlank() ? null : of(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
