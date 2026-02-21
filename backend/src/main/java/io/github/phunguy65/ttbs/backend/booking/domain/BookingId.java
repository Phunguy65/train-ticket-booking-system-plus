package io.github.phunguy65.ttbs.backend.booking.domain;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Objects;

public record BookingId(Long value) implements ValueObject {

    public BookingId {
        Objects.requireNonNull(value, "BookingId value must not be null");
    }
}
