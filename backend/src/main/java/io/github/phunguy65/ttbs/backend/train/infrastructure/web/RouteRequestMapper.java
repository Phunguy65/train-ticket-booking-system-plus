package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateRouteCommand;
import io.github.phunguy65.ttbs.backend.train.application.dto.RouteDto;
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
                request.basePrice());
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
}
