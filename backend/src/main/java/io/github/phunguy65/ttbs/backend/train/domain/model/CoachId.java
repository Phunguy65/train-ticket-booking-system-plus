package io.github.phunguy65.ttbs.backend.train.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/**
 * Type-safe value object wrapping the UUID primary key of a {@link Coach}.
 */
public record CoachId(UUID value) {

    public CoachId {
        if (value == null) {
            throw new IllegalArgumentException("CoachId value must not be null");
        }
    }

    public static CoachId of(UUID value) {
        return new CoachId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
