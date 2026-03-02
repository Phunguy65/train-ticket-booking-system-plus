package io.github.phunguy65.ttbs.backend.train.domain.errors;

import java.util.List;
import java.util.UUID;

/**
 * Typed business errors for the Route domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface RouteError {

    record RouteNotFound() implements RouteError {
        @Override
        public String message() {
            return "Route not found";
        }
    }

    /**
     * Returned by bulk delete when one or more IDs are not found.
     * Carries the list of missing IDs so the caller can report them.
     */
    record RoutesNotFound(List<UUID> invalidIds) implements RouteError {
        @Override
        public String message() {
            return "One or more routes were not found: " + invalidIds;
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
