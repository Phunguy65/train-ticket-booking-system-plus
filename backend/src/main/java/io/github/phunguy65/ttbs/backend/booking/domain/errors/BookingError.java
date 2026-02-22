package io.github.phunguy65.ttbs.backend.booking.domain.errors;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;

/**
 * Typed business errors that can occur within the Booking aggregate.
 *
 * <p>These are <em>not</em> exceptions – they are plain data values returned via
 * {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} so that callers can
 * pattern-match exhaustively without relying on exception control-flow.
 */
public sealed interface BookingError {

    /**
     * Attempted to confirm a booking that is not in {@code PENDING} status.
     *
     * @param currentStatus the actual status of the booking at the time of the attempt
     */
    record CannotConfirm(BookingStatus currentStatus) implements BookingError {
        @Override
        public String message() {
            return "Cannot confirm booking in status " + currentStatus
                    + ". Only PENDING bookings can be confirmed.";
        }
    }

    /**
     * Attempted to cancel a booking that is already {@code CANCELLED}.
     */
    record AlreadyCancelled() implements BookingError {
        @Override
        public String message() {
            return "Booking is already CANCELLED.";
        }
    }

    /** Human-readable description of the error, suitable for a JSend {@code fail} data payload. */
    String message();
}
