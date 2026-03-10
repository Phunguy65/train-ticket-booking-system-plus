package io.github.phunguy65.ttbs.backend.booking.domain.model;

/** Lifecycle states for a booking. */
public enum BookingStatus {
    /** Seats are held; awaiting payment within the payment deadline. */
    HELD,
    /** Payment received; booking is confirmed. */
    CONFIRMED,
    /** Booking was cancelled by the user or expired. */
    CANCELLED
}
