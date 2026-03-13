package io.github.phunguy65.ttbs.backend.station.infrastructure.web;

import io.github.phunguy65.ttbs.backend.station.application.command.CreateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.command.UpdateStationCommand;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.CreateStationHttpRequest;
import io.github.phunguy65.ttbs.backend.station.infrastructure.web.request.UpdateStationHttpRequest;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class StationRequestMapper {

    CreateStationCommand toCommand(CreateStationHttpRequest request) {
        return new CreateStationCommand(request.code(), request.name(), request.city());
    }

    StationHttpResponse toResponse(StationResponse dto) {
        return new StationHttpResponse(
                dto.id(), dto.code(), dto.name(), dto.city(), dto.createdAt());
    }

    UpdateStationCommand toUpdateCommand(UUID id, UpdateStationHttpRequest request) {
        return new UpdateStationCommand(
                StationId.of(id), request.code(), request.name(), request.city());
    }
}
