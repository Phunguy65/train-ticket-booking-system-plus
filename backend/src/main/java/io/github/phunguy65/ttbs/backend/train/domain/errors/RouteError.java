package io.github.phunguy65.ttbs.backend.train.domain.errors;

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

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
