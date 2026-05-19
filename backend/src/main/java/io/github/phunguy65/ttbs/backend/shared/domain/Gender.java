package io.github.phunguy65.ttbs.backend.shared.domain;

public record Gender(String value) implements ValueObject {

    public Gender {
        value = normalize(value, 20, "gender");
    }

    public static Gender of(String value) {
        return new Gender(value);
    }

    public static Gender ofNullable(String value) {
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
