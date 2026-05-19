package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.Objects;

public record IdempotencyKey(String value) implements ValueObject {

    public IdempotencyKey {
        Objects.requireNonNull(value, "idempotencyKey must not be null");
        value = value.trim();
        if (value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException(
                    "idempotencyKey must be between 1 and 255 characters");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
