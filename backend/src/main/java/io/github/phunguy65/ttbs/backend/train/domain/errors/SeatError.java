package io.github.phunguy65.ttbs.backend.train.domain.errors;

/**
 * Typed business errors for the Seat domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface SeatError {

    record SeatNotFound() implements SeatError {
        @Override
        public String message() {
            return "Seat not found";
        }
    }

    record SeatNumberAlreadyExists(String seatNumber) implements SeatError {
        @Override
        public String message() {
            return "A seat with number '" + seatNumber + "' already exists on this train";
        }
    }

    record TrainNotFound() implements SeatError {
        @Override
        public String message() {
            return "Train not found";
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
