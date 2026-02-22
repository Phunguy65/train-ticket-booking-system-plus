package io.github.phunguy65.ttbs.backend.shared.domain;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

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
