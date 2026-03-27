package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Locale;
import java.util.Objects;

public record SeatNumber(String value) implements ValueObject {

    public SeatNumber {
        Objects.requireNonNull(value, "seatNumber must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank() || value.length() > 10) {
            throw new IllegalArgumentException("seatNumber must be between 1 and 10 characters");
        }
    }

    public static SeatNumber of(String value) {
        return new SeatNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
