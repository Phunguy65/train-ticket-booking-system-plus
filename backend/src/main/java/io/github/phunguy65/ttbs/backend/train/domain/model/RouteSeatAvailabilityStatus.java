package io.github.phunguy65.ttbs.backend.train.domain.model;

/**
 * Per-route availability status of a seat.
 *
 * <p>Allowed transitions:
 * <ul>
 *   <li>AVAILABLE → HELD (via {@code hold()})
 *   <li>HELD → BOOKED (via {@code confirmHold()})
 *   <li>HELD → AVAILABLE (via {@code expire()})
 *   <li>AVAILABLE → BOOKED (via {@code book()})
 *   <li>BOOKED → CANCELLED (via {@code cancel()})
 *   <li>CANCELLED → AVAILABLE (via {@code release()})
 * </ul>
 */
public enum RouteSeatAvailabilityStatus {
    AVAILABLE,
    HELD,
    BOOKED,
    CANCELLED
}
