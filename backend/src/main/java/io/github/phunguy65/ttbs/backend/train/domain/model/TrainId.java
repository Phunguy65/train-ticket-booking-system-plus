package io.github.phunguy65.ttbs.backend.train.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

public record TrainId(UUID value) {

    public TrainId {
        if (value == null) {
            throw new IllegalArgumentException("TrainId value must not be null");
        }
    }

    public static TrainId of(UUID value) {
        return new TrainId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
