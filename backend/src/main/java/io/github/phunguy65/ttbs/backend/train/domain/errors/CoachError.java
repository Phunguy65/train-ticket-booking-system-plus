package io.github.phunguy65.ttbs.backend.train.domain.errors;

/**
 * Typed business errors for the Coach domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface CoachError {

    record CoachNotFound() implements CoachError {
        @Override
        public String message() {
            return "Coach not found";
        }
    }

    record CarNumberAlreadyExists(int carNumber) implements CoachError {
        @Override
        public String message() {
            return "A coach with car number '" + carNumber + "' already exists on this train";
        }
    }

    record TrainNotFound() implements CoachError {
        @Override
        public String message() {
            return "Train not found";
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
