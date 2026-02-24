package io.github.phunguy65.ttbs.backend.train.domain.errors;

/**
 * Typed business errors for the RouteSeatAvailability domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface RouteSeatAvailabilityError {

    record SeatNotAvailable() implements RouteSeatAvailabilityError {
        @Override
        public String message() {
            return "Seat is not available for booking on this route";
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
