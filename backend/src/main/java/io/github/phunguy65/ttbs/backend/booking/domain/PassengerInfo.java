package io.github.phunguy65.ttbs.backend.booking.domain;

import io.github.phunguy65.ttbs.backend.shared.domain.ValueObject;
import java.util.Objects;

public record PassengerInfo(String name, String email, String phone) implements ValueObject {

    public PassengerInfo {
        Objects.requireNonNull(name, "Passenger name must not be null");
        Objects.requireNonNull(email, "Passenger email must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Passenger name must not be blank");
        if (email.isBlank())
            throw new IllegalArgumentException("Passenger email must not be blank");
    }
}
