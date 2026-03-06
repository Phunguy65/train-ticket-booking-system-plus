package io.github.phunguy65.ttbs.backend.train.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

/**
 * Type-safe value object wrapping the UUID primary key of a route.
 *
 * <p>Shared across the train and booking modules as the canonical route identifier.
 */
public record RouteId(UUID value) {

    public RouteId {
        if (value == null) {
            throw new IllegalArgumentException("RouteId value must not be null");
        }
    }

    public static RouteId of(UUID value) {
        return new RouteId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
