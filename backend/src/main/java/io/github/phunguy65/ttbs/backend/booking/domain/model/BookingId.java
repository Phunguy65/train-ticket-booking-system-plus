package io.github.phunguy65.ttbs.backend.booking.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

public record BookingId(UUID value) {

    public BookingId {
        if (value == null) {
            throw new IllegalArgumentException("BookingId value must not be null");
        }
    }

    public static BookingId of(UUID value) {
        return new BookingId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
