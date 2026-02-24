package io.github.phunguy65.ttbs.backend.train.domain.model;

/**
 * Per-route availability status of a seat.
 *
 * <p>Allowed transitions:
 * <ul>
 *   <li>AVAILABLE → BOOKED (via {@code book()})
 *   <li>BOOKED → CANCELLED (via {@code cancel()})
 *   <li>CANCELLED → AVAILABLE (via {@code release()})
 * </ul>
 */
public enum RouteSeatAvailabilityStatus {
    AVAILABLE,
    BOOKED,
    CANCELLED
}
