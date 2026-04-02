package io.github.phunguy65.ttbs.backend.shared.infrastructure.cursor;

public class InvalidCursorException extends RuntimeException {

    public InvalidCursorException(String message, Throwable cause) {
        super(message, cause);
    }
}
