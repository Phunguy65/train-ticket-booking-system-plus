package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import java.time.Instant;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateRouteCommand(
        RouteId routeId,
        JsonNullable<Instant> departureTime,
        JsonNullable<Instant> arrivalTime,
        JsonNullable<Money> basePrice,
        JsonNullable<RouteStatus> status) {}
