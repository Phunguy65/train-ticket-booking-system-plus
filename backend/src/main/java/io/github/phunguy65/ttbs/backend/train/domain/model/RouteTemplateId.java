package io.github.phunguy65.ttbs.backend.train.domain.model;

import java.util.UUID;
import org.jspecify.annotations.NonNull;

public record RouteTemplateId(UUID value) {

    public RouteTemplateId {
        if (value == null) {
            throw new IllegalArgumentException("RouteTemplateId value must not be null");
        }
    }

    public static RouteTemplateId of(UUID value) {
        return new RouteTemplateId(value);
    }

    @Override
    @NonNull public String toString() {
        return value.toString();
    }
}
