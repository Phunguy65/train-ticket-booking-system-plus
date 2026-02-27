package io.github.phunguy65.ttbs.backend.user.domain.errors;

/**
 * Typed business errors for the User domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface UserError {

    record EmailAlreadyExists() implements UserError {
        @Override
        public String message() {
            return "An account with this email already exists";
        }
    }

    record InvalidCredentials() implements UserError {
        @Override
        public String message() {
            return "Invalid email or password";
        }
    }

    record InvalidRefreshToken() implements UserError {
        @Override
        public String message() {
            return "Invalid or expired refresh token";
        }
    }

    record UserNotFound() implements UserError {
        @Override
        public String message() {
            return "User not found";
        }
    }

    record UserAlreadyDeleted() implements UserError {
        @Override
        public String message() {
            return "User has already been deleted";
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
