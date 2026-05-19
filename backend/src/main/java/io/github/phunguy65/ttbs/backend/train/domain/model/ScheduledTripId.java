package io.github.phunguy65.ttbs.backend.train.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

public record ScheduledTripId(UUID value) {

    public ScheduledTripId {
        if (value == null) {
            throw new IllegalArgumentException("ScheduledTripId value must not be null");
        }
    }

    public static ScheduledTripId of(UUID value) {
        return new ScheduledTripId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
