package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Machine-readable error codes for all JSend {@code fail} responses.
 *
 * <p>Constants are module-namespaced (e.g. {@code USER_}, {@code BOOKING_}) to prevent collisions
 * and make provenance clear. {@code VALIDATION_ERROR} is the umbrella code used when Bean
 * Validation fails; individual field violations carry a {@link ViolationCode}.
 */
@Schema(description = "Machine-readable error code carried by shared fail responses.")
public enum ErrorCode {

    // ── Bean Validation umbrella ──────────────────────────────────────────────
    VALIDATION_ERROR,
    CURSOR_INVALID,

    // ── Authorization ─────────────────────────────────────────────────────────
    ACCESS_DENIED,

    // ── User module ───────────────────────────────────────────────────────────
    USER_EMAIL_ALREADY_EXISTS,
    USER_INVALID_CREDENTIALS,
    USER_INVALID_REFRESH_TOKEN,
    USER_NOT_FOUND,
    USER_CANNOT_BULK_DELETE_SELF,
    USER_HAS_ACTIVE_BOOKINGS,

    // ── Booking module ────────────────────────────────────────────────────────
    BOOKING_NOT_FOUND,
    BOOKING_CANNOT_CONFIRM,
    BOOKING_ALREADY_CANCELLED,

    // ── Train module ──────────────────────────────────────────────────────────
    TRAIN_NUMBER_ALREADY_EXISTS,
    TRAIN_NOT_FOUND,
    TRAIN_IN_USE,
    SEAT_NOT_FOUND,
    SEAT_NUMBER_ALREADY_EXISTS,
    SEAT_NOT_AVAILABLE,
    SEAT_IN_USE,
    SCHEDULED_TRIP_NOT_FOUND,
    COACH_NOT_FOUND,
    COACH_CAR_NUMBER_ALREADY_EXISTS,
    COACH_TRAIN_NOT_FOUND,
    COACH_IN_USE,
    COACH_CAR_NUMBERS_ALREADY_EXIST,
    COACH_DUPLICATE_CAR_NUMBERS_IN_REQUEST,
    SEAT_NUMBERS_ALREADY_EXIST,
    SEAT_DUPLICATE_SEAT_NUMBERS_IN_REQUEST,
    ROUTE_TEMPLATE_NOT_FOUND,

    // ── Station module ────────────────────────────────────────────────────────
    STATION_NOT_FOUND,
    STATION_CODE_ALREADY_EXISTS,
    STATION_IN_USE,

    // ── Payment module ────────────────────────────────────────────────────────
    PAYMENT_NOT_FOUND,
    PAYMENT_ALREADY_PROCESSED,
    PAYMENT_REFUND_FAILED
}
