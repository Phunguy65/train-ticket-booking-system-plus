package io.github.phunguy65.ttbs.backend.booking.domain.model;

import java.util.UUID;

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
    public String toString() {
        return value.toString();
    }
}
