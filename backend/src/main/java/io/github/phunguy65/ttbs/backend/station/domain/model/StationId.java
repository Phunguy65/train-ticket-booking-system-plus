package io.github.phunguy65.ttbs.backend.station.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/**
 * Type-safe value object wrapping the UUID primary key of a station.
 */
public record StationId(UUID value) {

    public StationId {
        if (value == null) {
            throw new IllegalArgumentException("StationId value must not be null");
        }
    }

    public static StationId of(UUID value) {
        return new StationId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
