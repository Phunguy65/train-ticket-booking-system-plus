package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchRouteRequest(
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

    public UpdateRouteCommand toCommand(UUID id) {
        JsonNullable<Money> basePriceMoney = basePrice.isPresent()
                ? JsonNullable.of(Money.vnd(basePrice.get()))
                : JsonNullable.undefined();
        return new UpdateRouteCommand(
                RouteId.of(id), departureTime, arrivalTime, basePriceMoney, status);
    }
}
