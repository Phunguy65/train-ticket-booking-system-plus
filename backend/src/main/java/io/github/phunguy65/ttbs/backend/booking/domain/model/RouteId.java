package io.github.phunguy65.ttbs.backend.booking.domain.model;

import java.util.UUID;

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
    public String toString() {
        return value.toString();
    }
}
