package io.github.phunguy65.ttbs.backend.booking.domain;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Objects;

public record BookingReference(String value) implements ValueObject {

    public BookingReference {
        Objects.requireNonNull(value, "BookingReference value must not be null");
        if (value.isBlank())
            throw new IllegalArgumentException("BookingReference must not be blank");
    }
}
