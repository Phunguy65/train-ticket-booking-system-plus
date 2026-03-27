package io.github.phunguy65.ttbs.backend.booking.domain.error;

/**
 * Typed business errors for the booking domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface BookingError {

    record BookingNotFound() implements BookingError {
        @Override
        public String message() {
            return "Booking not found";
        }
    }

    record SeatNotAvailable() implements BookingError {
        @Override
        public String message() {
            return "One or more requested seats are not available";
        }
    }

    record ActiveHoldExists() implements BookingError {
        @Override
        public String message() {
            return "An active hold already exists for this user on this scheduled trip";
        }
    }

    record InvalidStatusTransition(String from, String to) implements BookingError {
        @Override
        public String message() {
            return "Cannot transition booking from " + from + " to " + to;
        }
    }

    record Forbidden() implements BookingError {
        @Override
        public String message() {
            return "You are not allowed to perform this action on this booking";
        }
    }

    record ScheduledTripNotFound() implements BookingError {
        @Override
        public String message() {
            return "Scheduled trip not found";
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
