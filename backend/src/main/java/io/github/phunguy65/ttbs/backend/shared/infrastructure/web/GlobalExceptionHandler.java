package io.github.phunguy65.ttbs.backend.shared.infrastructure.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
 *   <li>{@link MethodArgumentNotValidException} → 400 {@code fail} (Bean Validation errors)
 *   <li>{@link Exception} → 500 {@code error} (catch-all, hides implementation details)
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures (@Valid on request bodies).
     * Returns JSend {@code fail} with a {@link FailData} containing a populated {@code errors} list.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<JsendResponse<FailData>> handleValidationException(
            MethodArgumentNotValidException ex) {
        List<Violation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new Violation(
                        fe.getField(), fe.getDefaultMessage(), resolveViolationCode(fe)))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(JsendResponse.fail(
                        new FailData("Validation failed", ErrorCode.VALIDATION_ERROR, violations)));
    }

    /**
     * Handles optimistic locking failures — two concurrent requests modified the same entity.
     * Returns JSend {@code fail} with HTTP 409 Conflict.
     *
     * <p>This typically happens when two concurrent booking requests race to book the same seat.
     * The client should retry.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<JsendResponse<FailData>> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(JsendResponse.fail(new FailData(
                        "The resource was modified concurrently. Please retry.",
                        ErrorCode.SEAT_NOT_AVAILABLE,
                        List.of())));
    }

    /**
     * Handles Spring Security method-level authorization denials (@PreAuthorize).
     * Returns JSend {@code fail} with HTTP 403.
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    ResponseEntity<JsendResponse<FailData>> handleAuthorizationDenied(
            AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(JsendResponse.fail(
                        new FailData("Access denied", ErrorCode.ACCESS_DENIED, List.of())));
    }

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

    /**
     * Maps a Spring {@link FieldError} to the appropriate {@link ViolationCode} by inspecting
     * the underlying Jakarta Validation constraint annotation type.
     */
    private ViolationCode resolveViolationCode(FieldError fe) {
        try {
            ConstraintViolation<?> cv = fe.unwrap(ConstraintViolation.class);
            Class<?> annotationType =
                    cv.getConstraintDescriptor().getAnnotation().annotationType();

            if (annotationType == NotBlank.class
                    || annotationType == NotNull.class
                    || annotationType == NotEmpty.class) {
                return ViolationCode.REQUIRED;
            }
            if (annotationType == Email.class || annotationType == Pattern.class) {
                return ViolationCode.INVALID_FORMAT;
            }
            if (annotationType == Size.class) {
                Object rejected = fe.getRejectedValue();
                int length = (rejected instanceof String s) ? s.length() : 0;
                int min = (int) cv.getConstraintDescriptor().getAttributes().getOrDefault("min", 0);
                return (length < min) ? ViolationCode.TOO_SHORT : ViolationCode.TOO_LONG;
            }
        } catch (Exception ignored) {
            // Fall through to default if unwrap or attribute access fails
        }
        return ViolationCode.INVALID_VALUE;
    }
}
