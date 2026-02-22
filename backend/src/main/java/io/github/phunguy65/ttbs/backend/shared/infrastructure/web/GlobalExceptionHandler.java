package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates <em>technical</em> exceptions into JSend-compliant HTTP responses.
 *
 * <p>Business/domain errors must NOT reach this handler – they are represented as
 * {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} values and translated
 * to JSend {@code fail} responses directly in each controller endpoint.
 *
 * <p>Handler mapping:
 * <ul>
 *   <li>{@link Exception} → 500 {@code error} (catch-all, hides implementation details)
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Catch-all for any unhandled technical exception.
     * Returns a JSend {@code error} with HTTP 500 and a generic message to avoid
     * leaking stack-trace information to clients.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<JsendResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(JsendResponse.error("An unexpected error occurred. Please try again later."));
    }
}
