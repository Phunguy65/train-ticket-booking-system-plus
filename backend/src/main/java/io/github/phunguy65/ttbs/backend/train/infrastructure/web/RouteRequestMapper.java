package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

@Component
class RouteRequestMapper {

    CreateRouteCommand toCommand(CreateRouteHttpRequest request) {
        return new CreateRouteCommand(
                request.trainId(),
                request.originStationId(),
                request.destinationStationId(),
                request.departureTime(),
                request.arrivalTime(),
                Money.vnd(request.basePrice()));
    }

    RouteHttpResponse toResponse(RouteDto dto) {
        return new RouteHttpResponse(
                dto.id(),
                dto.trainId(),
                dto.originStationId(),
                dto.destinationStationId(),
                dto.departureTime(),
                dto.arrivalTime(),
                dto.basePrice(),
                dto.status(),
                dto.createdAt());
    }

    UpdateRouteCommand toUpdateCommand(UUID id, UpdateRouteHttpRequest request) {
        JsonNullable<Money> basePrice = request.basePrice().isPresent()
                ? JsonNullable.of(Money.vnd(request.basePrice().get()))
                : JsonNullable.undefined();
        return new UpdateRouteCommand(
                RouteId.of(id),
                request.departureTime(),
                request.arrivalTime(),
                basePrice,
                request.status());
    }
}
