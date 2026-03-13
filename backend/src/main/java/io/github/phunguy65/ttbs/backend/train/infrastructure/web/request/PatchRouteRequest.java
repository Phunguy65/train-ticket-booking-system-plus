package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

record PatchRouteRequest(
        JsonNullable<Instant> departureTime,
        JsonNullable<Instant> arrivalTime,
        @PositiveOrZero JsonNullable<Long> basePrice,
        JsonNullable<RouteStatus> status) {

    PatchRouteRequest() {
        this(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined());
    }
}
