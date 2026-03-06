package io.github.phunguy65.ttbs.backend.booking.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/** Type-safe UUID wrapper for a user ID within the booking module boundary. */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId value must not be null");
        }
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
