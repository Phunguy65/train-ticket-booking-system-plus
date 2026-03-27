package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.Locale;
import java.util.Objects;

public record EmailAddress(String value) implements ValueObject {

    public EmailAddress {
        Objects.requireNonNull(value, "email must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()
                || !value.contains("@")
                || value.startsWith("@")
                || value.endsWith("@")) {
            throw new IllegalArgumentException("email must be a valid address");
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
