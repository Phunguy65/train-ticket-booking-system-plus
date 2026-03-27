package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.Objects;

public record PasswordHash(String value) implements ValueObject {

    public PasswordHash {
        Objects.requireNonNull(value, "passwordHash must not be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
