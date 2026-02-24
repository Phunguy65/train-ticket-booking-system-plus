package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

/**
 * Machine-readable error codes for all JSend {@code fail} responses.
 *
 * <p>Constants are module-namespaced (e.g. {@code USER_}, {@code BOOKING_}) to prevent collisions
 * and make provenance clear. {@code VALIDATION_ERROR} is the umbrella code used when Bean
 * Validation fails; individual field violations carry a {@link ViolationCode}.
 */
public enum ErrorCode {

    // ── Bean Validation umbrella ──────────────────────────────────────────────
    VALIDATION_ERROR,

    // ── Authorization ─────────────────────────────────────────────────────────
    ACCESS_DENIED,

    // ── User module ───────────────────────────────────────────────────────────
    USER_EMAIL_ALREADY_EXISTS,
    USER_INVALID_CREDENTIALS,
    USER_INVALID_REFRESH_TOKEN,
    USER_NOT_FOUND,

    // ── Booking module ────────────────────────────────────────────────────────
    BOOKING_NOT_FOUND,
    BOOKING_CANNOT_CONFIRM,
    BOOKING_ALREADY_CANCELLED
}
