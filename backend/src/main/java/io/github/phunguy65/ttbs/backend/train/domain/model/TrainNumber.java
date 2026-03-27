package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Locale;
import java.util.Objects;

public record TrainNumber(String value) implements ValueObject {

    public TrainNumber {
        Objects.requireNonNull(value, "trainNumber must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank() || value.length() > 20) {
            throw new IllegalArgumentException("trainNumber must be between 1 and 20 characters");
        }
    }

    public static TrainNumber of(String value) {
        return new TrainNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
