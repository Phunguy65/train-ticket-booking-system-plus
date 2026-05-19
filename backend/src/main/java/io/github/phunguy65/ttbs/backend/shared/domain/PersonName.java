package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.Objects;

public record PersonName(String value) implements ValueObject {

    public PersonName {
        Objects.requireNonNull(value, "name must not be null");
        value = value.trim().replaceAll("\\s+", " ");
        if (value.isBlank() || value.length() > 255) {
            throw new IllegalArgumentException("name must be between 1 and 255 characters");
        }
    }

    public static PersonName of(String value) {
        return new PersonName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
