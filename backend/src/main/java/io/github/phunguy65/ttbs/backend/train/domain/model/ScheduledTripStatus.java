package io.github.phunguy65.ttbs.backend.train.domain.model;

/**
 * Status lifecycle for a scheduled trip instance.
 *
 * <p>Separates the operational execution state of a concrete departure from the reusable route
 * template that defines the shared journey pattern.
 */
public enum ScheduledTripStatus {
    SCHEDULED,
    BOARDING,
    DEPARTED,
    ARRIVED,
    CANCELLED
}
