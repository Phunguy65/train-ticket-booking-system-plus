package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

record UpdateRouteHttpRequest(
        JsonNullable<Instant> departureTime,
        JsonNullable<Instant> arrivalTime,
        @PositiveOrZero JsonNullable<BigDecimal> basePrice,
        JsonNullable<RouteStatus> status) {

    UpdateRouteHttpRequest() {
        this(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined());
    }
}
