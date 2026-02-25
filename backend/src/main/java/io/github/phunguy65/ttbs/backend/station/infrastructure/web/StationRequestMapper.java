package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import io.github.phunguy65.ttbs.backend.station.application.command.CreateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.dto.StationDto;
import org.springframework.stereotype.Component;

@Component
class StationRequestMapper {

    CreateStationCommand toCommand(CreateStationHttpRequest request) {
        return new CreateStationCommand(request.code(), request.name(), request.city());
    }

    StationHttpResponse toResponse(StationDto dto) {
        return new StationHttpResponse(
                dto.id(), dto.code(), dto.name(), dto.city(), dto.createdAt());
    }
}
