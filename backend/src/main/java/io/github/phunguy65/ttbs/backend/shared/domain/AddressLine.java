package io.github.phunguy65.ttbs.backend.shared.domain;

public record AddressLine(String value) implements ValueObject {

    public AddressLine {
        value = normalize(value, 255, "address line");
    }

    public static AddressLine of(String value) {
        return new AddressLine(value);
    }

    public static AddressLine ofNullable(String value) {
        return value == null || value.isBlank() ? null : of(value);
    }

    private static String normalize(String value, int maxLength, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }
}
