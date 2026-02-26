package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateRouteCommand(
        RouteId routeId,
        JsonNullable<Instant> departureTime,
        JsonNullable<Instant> arrivalTime,
        JsonNullable<BigDecimal> basePrice,
        JsonNullable<RouteStatus> status) {}
