package io.github.phunguy65.ttbs.backend.station.domain.error;

import java.util.List;
import java.util.UUID;

/**
 * Typed business errors for the Station domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface StationError {

    record StationNotFound() implements StationError {
        @Override
        public String message() {
            return "Station not found";
        }
    }

    record StationCodeAlreadyExists(String code) implements StationError {
        @Override
        public String message() {
            return "A station with code '" + code + "' already exists";
        }
    }

    record StationInUse(List<UUID> conflictingIds) implements StationError {
        @Override
        public String message() {
            return "One or more stations are referenced by active routes and cannot be deleted";
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
