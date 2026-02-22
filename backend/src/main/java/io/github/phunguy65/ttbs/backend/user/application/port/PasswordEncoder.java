package io.github.phunguy65.ttbs.backend.user.application.port;

/**
 * Port for password hashing operations.
 * Implemented by a BCrypt adapter in the security infrastructure layer.
 */
public interface PasswordEncoder {

    /** Returns the BCrypt hash of the raw password. */
    String encode(String rawPassword);

    /** Returns true if rawPassword matches the stored encodedPassword hash. */
    boolean matches(String rawPassword, String encodedPassword);
}
