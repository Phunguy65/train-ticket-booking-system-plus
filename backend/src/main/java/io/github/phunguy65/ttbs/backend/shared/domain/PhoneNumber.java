package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.Objects;

public record PhoneNumber(String value) implements ValueObject {

    public PhoneNumber {
        Objects.requireNonNull(value, "phone must not be null");
        value = normalize(value);
        if (value.isBlank() || value.length() < 8 || value.length() > 20) {
            throw new IllegalArgumentException("phone must be between 8 and 20 characters");
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    public static PhoneNumber ofNullable(String value) {
        return value == null || value.isBlank() ? null : of(value);
    }

    private static String normalize(String raw) {
        String trimmed =
                raw.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.substring(1).replaceAll("[^0-9]", "");
        }
        return trimmed.replaceAll("[^0-9]", "");
    }

    @Override
    public String toString() {
        return value;
    }
}
