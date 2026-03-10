package io.github.phunguy65.ttbs.backend.train.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/**
 * Type-safe value object wrapping the UUID primary key of a {@link Seat}.
 */
public record SeatId(UUID value) {

    public SeatId {
        if (value == null) {
            throw new IllegalArgumentException("SeatId value must not be null");
        }
    }

    public static SeatId of(UUID value) {
        return new SeatId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
