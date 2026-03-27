package io.github.phunguy65.ttbs.backend.train.domain.error;

/**
 * Typed business errors for the scheduled trip domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} - never thrown.
 */
public sealed interface ScheduledTripError {

    record ScheduledTripNotFound() implements ScheduledTripError {
        @Override
        public String message() {
            return "Scheduled trip not found";
        }
    }

    String message();
}
