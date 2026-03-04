package io.github.phunguy65.ttbs.backend.payment.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;

public record CheckoutSessionId(String value) implements ValueObject {

    public static CheckoutSessionId of(String value) {
        return new CheckoutSessionId(value);
    }
}
