package io.github.phunguy65.ttbs.backend.train.application.query;

public enum ScheduledTripSearchSortField {
    DEPARTURE_TIME,
    PRICE,
    DURATION,
    AVAILABLE_SEATS;

    public static ScheduledTripSearchSortField from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DEPARTURE_TIME;
        }
        try {
            return ScheduledTripSearchSortField.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DEPARTURE_TIME;
        }
    }
}
