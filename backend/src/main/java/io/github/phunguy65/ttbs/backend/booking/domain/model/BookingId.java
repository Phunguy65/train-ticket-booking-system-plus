package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.shared.infrastructure.IdGenerator;
import java.util.UUID;

public record BookingId(UUID value) {

    public BookingId {
        if (value == null) {
            throw new IllegalArgumentException("BookingId value must not be null");
        }
    }

    public static BookingId generate() {
        return new BookingId(UUID.fromString(IdGenerator.generate()));
    }

    public static BookingId of(UUID value) {
        return new BookingId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
