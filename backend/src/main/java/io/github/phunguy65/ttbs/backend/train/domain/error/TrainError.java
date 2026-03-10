package io.github.phunguy65.ttbs.backend.train.domain.error;

import java.util.List;
import java.util.UUID;

/**
 * Typed business errors for the Train domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface TrainError {

    record TrainNumberAlreadyExists(String trainNumber) implements TrainError {
        @Override
        public String message() {
            return "A train with number '" + trainNumber + "' already exists";
        }
    }

    record TrainNotFound() implements TrainError {
        @Override
        public String message() {
            return "Train not found";
        }
    }

    record TrainInUse(List<UUID> conflictingIds) implements TrainError {
        @Override
        public String message() {
            return "One or more trains are referenced by active routes and cannot be deleted";
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
