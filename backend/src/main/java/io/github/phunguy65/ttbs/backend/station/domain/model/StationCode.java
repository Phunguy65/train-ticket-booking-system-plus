package io.github.phunguy65.ttbs.backend.station.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Locale;
import java.util.Objects;

public record StationCode(String value) implements ValueObject {

    public StationCode {
        Objects.requireNonNull(value, "stationCode must not be null");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank() || value.length() > 20) {
            throw new IllegalArgumentException("stationCode must be between 1 and 20 characters");
        }
    }

    public static StationCode of(String value) {
        return new StationCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
